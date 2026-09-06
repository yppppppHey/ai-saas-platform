package com.aisaas.gateway.filter;

import com.aisaas.gateway.config.GatewayConfig;
import com.aisaas.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.server.ServerWebExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT认证过滤器
 * 负责验证Token有效性、提取用户信息并传递给下游服务
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private GatewayConfig gatewayConfig;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        
        log.debug("JWT Auth Filter - Path: {}, Method: {}", path, method);

        // 1. 检查是否是白名单路径（无需认证）
        if (isWhiteListPath(path)) {
            log.debug("Path {} is in whitelist, skipping authentication", path);
            return chain.filter(exchange);
        }

        // 2. 检查是否是匿名访问路径
        boolean isAnonymous = isAnonymousPath(path);

        // 3. 获取Token
        String token = extractToken(request);
        
        if (!StringUtils.hasText(token)) {
            if (isAnonymous) {
                // 匿名路径允许无Token访问
                log.debug("Anonymous path accessed without token: {}", path);
                return chain.filter(exchange);
            }
            log.warn("No token found for path: {}", path);
            return unauthorized(response, "未提供有效的认证令牌");
        }

        // 4. 验证Token
        Claims claims = jwtUtil.parseToken(token);
        
        if (claims == null) {
            log.warn("Invalid token for path: {}", path);
            return unauthorized(response, "认证令牌无效或已过期");
        }

        // 5. 检查Token是否即将过期（可选：在这里刷新Token）
        if (jwtUtil.isTokenExpiringSoon(token, 300)) { // 5分钟内过期
            log.debug("Token is expiring soon for user: {}", claims.get("userId"));
            // 可以在这里添加Token刷新逻辑
        }

        // 6. 提取用户信息
        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        String tokenType = claims.get("tokenType", String.class);

        // 7. 检查Token类型（访问令牌 vs 刷新令牌）
        if (!"access".equals(tokenType)) {
            log.warn("Invalid token type for path {}: expected access, got {}", path, tokenType);
            return unauthorized(response, "令牌类型不正确");
        }

        // 8. 构建新的请求，添加用户信息到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username != null ? username : "")
                .header("X-User-Role", role != null ? role : "")
                .header("X-Trace-Id", java.util.UUID.randomUUID().toString())
                .build();

        log.debug("Authenticated user {} for path: {}", userId, path);

        // 9. 继续处理请求
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 检查是否是白名单路径
     * 该方法用于判断请求路径是否在白名单中，如果在白名单中则允许访问
     *
     * @param path 需要检查的请求路径
     * @return 如果路径在白名单中返回true，否则返回false
     */
    private boolean isWhiteListPath(String path) {
        // 从网关配置中获取白名单列表
        List<String> whiteList = gatewayConfig.getWhiteList();
        // 如果配置中的白名单为空，则使用默认白名单
        if (CollectionUtils.isEmpty(whiteList)) {
            // 默认白名单，包含一些不需要验证的路径，如认证接口、注册接口、健康检查接口等
            whiteList = List.of(
                    "/api/v1/auth/**",      // 认证相关接口
                    "/api/v1/users/register", // 用户注册接口
                    "/actuator/**",         // Spring Boot Actuator监控端点
                    "/health",              // 健康检查接口
                    "/swagger-ui/**",       // Swagger UI文档
                    "/v3/api-docs/**"       // OpenAPI文档
            );
        }

        // 使用路径匹配器检查当前路径是否匹配白名单中的任意一个模式
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 检查是否是匿名访问路径
     */
    private boolean isAnonymousPath(String path) {
        List<String> anonymousPaths = gatewayConfig.getAnonymousPaths();
        if (CollectionUtils.isEmpty(anonymousPaths)) {
            return false;
        }
        return anonymousPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        // 1. 从Authorization头中提取
        String bearerToken = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 从自定义头X-Token中提取
        String customToken = headers.getFirst("X-Token");
        if (StringUtils.hasText(customToken)) {
            return customToken;
        }

        // 3. 从查询参数中提取（不推荐，仅用于特殊场景）
        String paramToken = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message,
                System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10; // 确保在日志过滤器之后，其他过滤器之前
    }
}
