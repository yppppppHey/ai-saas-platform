package com.aisaas.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 访问日志过滤器
 * 记录详细的请求/响应日志和性能指标
 */
@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_KEY = "startTime";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String REQUEST_BODY_KEY = "requestBody";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_KEY, startTime);

        // 生成或获取TraceId
        String traceId = getOrCreateTraceId(exchange);
        exchange.getAttributes().put(TRACE_ID_KEY, traceId);

        // 获取请求信息
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethodValue();
        String path = request.getPath().value();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);

        // 记录请求日志
        if (log.isDebugEnabled()) {
            log.debug("[{}] {} {} - Client: {} - UA: {}",
                    traceId, method, path, clientIp, userAgent);
        }

        // 包装响应以捕获响应信息
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        // 创建响应包装器
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        // 读取响应内容
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);

                        // 记录响应日志
                        long duration = System.currentTimeMillis() - startTime;
                        logAccess(exchange, request, originalResponse, duration, content);

                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        // 正常处理请求
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .doFinally(signalType -> {
                    // 确保即使响应没有被writeWith处理，也能记录日志
                    if (!exchange.getAttributes().containsKey("access_logged")) {
                        long duration = System.currentTimeMillis() - startTime;
                        logAccess(exchange, request, originalResponse, duration, null);
                    }
                });
    }

    /**
     * 获取或创建TraceId
     */
    private String getOrCreateTraceId(ServerWebExchange exchange) {
        // 1. 从请求头中获取
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }

        // 2. 从请求属性中获取（可能已经由上游设置）
        Object existingTraceId = exchange.getAttribute(TRACE_ID_KEY);
        if (existingTraceId != null) {
            return existingTraceId.toString();
        }

        // 3. 生成新的TraceId
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (!StringUtils.hasText(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (!StringUtils.hasText(ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip)) {
            java.net.InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null) {
                ip = remoteAddress.getAddress().getHostAddress();
            }
        }
        
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    /**
     * 记录访问日志
     */
    private void logAccess(ServerWebExchange exchange, ServerHttpRequest request, 
                          ServerHttpResponse response, long duration, byte[] responseBody) {
        // 标记已记录
        exchange.getAttributes().put("access_logged", true);
        
        String traceId = exchange.getAttribute(TRACE_ID_KEY);
        String method = request.getMethodValue();
        String path = request.getPath().value();
        String query = request.getURI().getQuery();
        String clientIp = getClientIp(request);
        org.springframework.http.HttpStatus status = response.getStatusCode();
        int statusCode = status != null ? status.value() : 0;
        
        // 获取路由信息
        org.springframework.cloud.gateway.route.Route route = exchange.getAttribute(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        
        // 构建日志消息
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("[ACCESS] ")
              .append("TraceId=").append(traceId).append(" | ")
              .append("Method=").append(method).append(" | ")
              .append("Path=").append(path);
        
        if (StringUtils.hasText(query)) {
            logMsg.append("?").append(query);
        }
        
        logMsg.append(" | ")
              .append("Status=").append(statusCode).append(" | ")
              .append("Duration=").append(duration).append("ms | ")
              .append("ClientIp=").append(clientIp).append(" | ")
              .append("Route=").append(routeId);
        
        // 根据状态码和耗时选择日志级别
        if (statusCode >= 500) {
            log.error(logMsg.toString());
        } else if (statusCode >= 400) {
            log.warn(logMsg.toString());
        } else if (duration > 1000) {
            log.warn("{} [SLOW REQUEST]", logMsg.toString());
        } else {
            log.info(logMsg.toString());
        }
        
        // 记录详细响应内容（仅用于调试，生产环境需要关闭）
        if (log.isDebugEnabled() && responseBody != null && responseBody.length > 0) {
            String responseBodyStr = new String(responseBody, StandardCharsets.UTF_8);
            if (responseBodyStr.length() > 1000) {
                responseBodyStr = responseBodyStr.substring(0, 1000) + "... [truncated]";
            }
            log.debug("[RESPONSE BODY] TraceId={} Body={}", traceId, responseBodyStr);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // 在最高优先级之后
    }
}
