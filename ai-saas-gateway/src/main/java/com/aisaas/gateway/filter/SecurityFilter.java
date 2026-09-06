package com.aisaas.gateway.filter;

import com.aisaas.gateway.config.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 安全过滤器
 * 提供IP黑白名单、XSS防护、SQL注入防护、请求大小限制等安全功能
 */
@Slf4j
@Component
public class SecurityFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_KEY = "traceId";

    // XSS攻击模式
    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[^\n]*=[\r\n]*\\'(.*?)\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[^\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("<iframe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<form", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<input", Pattern.CASE_INSENSITIVE)
    };

    // SQL注入模式
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            Pattern.compile("('.+--)|(--\\s)|(;\\s*--)|(;\\s*\\/\\*)|(\\*\\/\\s*;)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|TRUNCATE|REPLACE|MERGE|CALL|DESCRIBE|SHOW|USE|SET|DECLARE|PREPARE|EXECUTE|DEALLOCATE)\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b(OR|AND)\\s+\\d+\\s*[=<>]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b(SLEEP|BENCHMARK|PG_SLEEP|WAITFOR|DELAY)\\s*\\()")
    };

    // 路径遍历模式
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\.)|(\\\\)|(%2e%2e)|(%252e%252e)", Pattern.CASE_INSENSITIVE);

    // 敏感文件访问模式
    private static final Pattern[] SENSITIVE_FILE_PATTERNS = {
            Pattern.compile("\\.(git|svn|hg|bzr|DS_Store|env|env\\.local|env\\.production|env\\.development)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(web\\.config|app\\.config|settings\\.json|appsettings\\.json|secrets\\.json)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.(bak|backup|old|orig|save|swp|tmp)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(id_rsa|id_dsa|id_ecdsa|id_ed25519|\\.pem|\\.key)$", Pattern.CASE_INSENSITIVE)
    };

    @Autowired
    private GatewayConfig gatewayConfig;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request);

        // 1. IP黑名单检查
        if (isIpBlacklisted(clientIp)) {
            log.warn("Request from blacklisted IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "IP地址已被列入黑名单");
        }

        // 2. IP白名单检查（如果启用了白名单）
        if (isIpWhitelistEnabled() && !isIpWhitelisted(clientIp)) {
            log.warn("Request from non-whitelisted IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "IP地址不在白名单中");
        }

        // 3. 路径遍历攻击检查
        if (containsPathTraversal(path)) {
            log.warn("Path traversal attack detected from IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "非法路径访问");
        }

        // 4. 敏感文件访问检查
        if (isSensitiveFileAccess(path)) {
            log.warn("Sensitive file access attempt from IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "禁止访问的资源");
        }

        // 5. 请求大小限制检查
        if (isRequestTooLarge(request)) {
            log.warn("Request too large from IP: {} - Path: {}", clientIp, path);
            return payloadTooLarge(exchange);
        }

        // 6. XSS攻击检查（仅对特定方法和Content-Type）
        if (shouldCheckXss(request)) {
            return checkXssAndContinue(exchange, chain, clientIp, path);
        }

        // 7. SQL注入检查
        if (shouldCheckSqlInjection(request)) {
            return checkSqlInjectionAndContinue(exchange, chain, clientIp, path);
        }

        // 继续处理请求
        return chain.filter(exchange);
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
     * 检查IP是否在黑名单中
     */
    private boolean isIpBlacklisted(String ip) {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        if (security == null || CollectionUtils.isEmpty(security.getIpBlacklist())) {
            return false;
        }
        return security.getIpBlacklist().contains(ip);
    }

    /**
     * 检查IP是否在白名单中
     */
    private boolean isIpWhitelisted(String ip) {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        if (security == null || CollectionUtils.isEmpty(security.getIpWhitelist())) {
            return false;
        }
        return security.getIpWhitelist().contains(ip);
    }

    /**
     * 是否启用了IP白名单
     */
    private boolean isIpWhitelistEnabled() {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        return security != null && security.isIpWhitelistEnabled();
    }

    /**
     * 检查路径是否包含路径遍历
     */
    private boolean containsPathTraversal(String path) {
        return PATH_TRAVERSAL_PATTERN.matcher(path).find();
    }

    /**
     * 检查是否是敏感文件访问
     */
    private boolean isSensitiveFileAccess(String path) {
        for (Pattern pattern : SENSITIVE_FILE_PATTERNS) {
            if (pattern.matcher(path).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查请求是否过大
     */
    private boolean isRequestTooLarge(ServerHttpRequest request) {
        String contentLength = request.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
        if (StringUtils.hasText(contentLength)) {
            try {
                long length = Long.parseLong(contentLength);
                // 默认限制100MB
                return length > 100 * 1024 * 1024;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 是否应该检查XSS
     */
    private boolean shouldCheckXss(ServerHttpRequest request) {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        if (security == null || !security.isXssProtection()) {
            return false;
        }
        
        String method = request.getMethod().name();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return false;
        }
        
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.hasText(contentType)) {
            return contentType.contains(MediaType.APPLICATION_JSON_VALUE) ||
                   contentType.contains(MediaType.TEXT_PLAIN_VALUE);
        }
        
        return false;
    }

    /**
     * 检查XSS并继续
     */
    private Mono<Void> checkXssAndContinue(ServerWebExchange exchange, GatewayFilterChain chain,
                                           String clientIp, String path) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    
                    // 检查XSS攻击
                    for (Pattern pattern : XSS_PATTERNS) {
                        if (pattern.matcher(body).find()) {
                            log.warn("XSS attack detected from IP: {} - Path: {}", clientIp, path);
                            return forbidden(exchange, "检测到XSS攻击");
                        }
                    }
                    
                    // 重新包装请求体
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                        }
                    };
                    
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * 是否应该检查SQL注入
     */
    private boolean shouldCheckSqlInjection(ServerHttpRequest request) {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        if (security == null || !security.isSqlInjectionProtection()) {
            return false;
        }
        
        String method = request.getMethod().name();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method) && !"GET".equals(method)) {
            return false;
        }
        
        // 检查查询参数
        if (!request.getQueryParams().isEmpty()) {
            return true;
        }
        
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.hasText(contentType)) {
            return contentType.contains(MediaType.APPLICATION_JSON_VALUE) ||
                   contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        }
        
        return false;
    }

    /**
     * 检查SQL注入并继续
     */
    private Mono<Void> checkSqlInjectionAndContinue(ServerWebExchange exchange, GatewayFilterChain chain,
                                                    String clientIp, String path) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 检查查询参数
        if (!request.getQueryParams().isEmpty()) {
            for (List<String> values : request.getQueryParams().values()) {
                for (String value : values) {
                    if (containsSqlInjection(value)) {
                        log.warn("SQL injection in query params from IP: {} - Path: {}", clientIp, path);
                        return forbidden(exchange, "检测到SQL注入攻击");
                    }
                }
            }
        }
        
        // 检查请求体
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    
                    if (containsSqlInjection(body)) {
                        log.warn("SQL injection in request body from IP: {} - Path: {}", clientIp, path);
                        return forbidden(exchange, "检测到SQL注入攻击");
                    }
                    
                    // 重新包装请求体
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                        }
                    };
                    
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * 检查是否包含SQL注入
     */
    private boolean containsSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String traceId = exchange.getAttribute(TRACE_ID_KEY);
        
        String body = String.format(
                "{\"code\":403,\"message\":\"%s\",\"data\":null,\"traceId\":\"%s\",\"timestamp\":%d}",
                message,
                traceId != null ? traceId : "",
                System.currentTimeMillis()
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> payloadTooLarge(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getAttribute(TRACE_ID_KEY);

        String body = String.format(
                "{\"code\":413,\"message\":\"请求体过大\",\"data\":null,\"traceId\":\"%s\",\"timestamp\":%d}",
                traceId != null ? traceId : "",
                System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3; // 在Token黑名单过滤器之后
    }
}
