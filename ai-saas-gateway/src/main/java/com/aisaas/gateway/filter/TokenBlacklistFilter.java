package com.aisaas.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单过滤器
 * 负责检查Token是否已被注销或列入黑名单
 */
@Slf4j
@Component
public class TokenBlacklistFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String TOKEN_JTI_PREFIX = "token:jti:";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_TOKEN_HEADER = "X-Token";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange);
        
        if (!StringUtils.hasText(token)) {
            // 无Token请求，直接放行（由JWT过滤器处理）
            return chain.filter(exchange);
        }

        // 检查Token是否在黑名单中
        if (isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted: {}", maskToken(token));
            return forbidden(exchange, "令牌已被注销");
        }

        // 检查Token的唯一标识（JTI）是否有效
        String jti = getTokenJti(token);
        if (StringUtils.hasText(jti) && !isJtiValid(jti)) {
            log.warn("Token JTI is invalid: {}", jti);
            return forbidden(exchange, "令牌已失效");
        }

        return chain.filter(exchange);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerWebExchange exchange) {
        // 1. 从Authorization头中提取
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // 2. 从X-Token头中提取
        String xToken = exchange.getRequest().getHeaders().getFirst(X_TOKEN_HEADER);
        if (StringUtils.hasText(xToken)) {
            return xToken;
        }

        // 3. 从查询参数中提取
        String paramToken = exchange.getRequest().getQueryParams().getFirst("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            // 使用Token的哈希作为黑名单键
            String tokenHash = hashToken(token);
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + tokenHash;
            
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            return Boolean.TRUE.equals(isBlacklisted);
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取Token的JTI（JWT ID）
     */
    private String getTokenJti(String token) {
        try {
            // 解析Token获取JTI
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Base64解码payload
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            
            // 简单解析JSON获取jti
            int jtiStart = payload.indexOf("\"jti\":");
            if (jtiStart > 0) {
                int valueStart = payload.indexOf("\"", jtiStart + 6) + 1;
                int valueEnd = payload.indexOf("\"", valueStart);
                return payload.substring(valueStart, valueEnd);
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Error extracting JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查JTI是否有效
     */
    private boolean isJtiValid(String jti) {
        try {
            String jtiKey = TOKEN_JTI_PREFIX + jti;
            Boolean exists = redisTemplate.hasKey(jtiKey);
            // 如果JTI存在，说明是有效的
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking JTI validity: {}", e.getMessage());
            // 出错时默认允许通过
            return true;
        }
    }

    /**
     * 对Token进行哈希
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            // 出错时返回原始Token的前16位
            return token.length() > 16 ? token.substring(0, 16) : token;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 掩码显示Token（用于日志）
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":403,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message,
                System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5; // 在JWT过滤器之前执行
    }
}
