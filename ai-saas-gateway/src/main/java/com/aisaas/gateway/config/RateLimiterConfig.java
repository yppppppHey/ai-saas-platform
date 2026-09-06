package com.aisaas.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 限流器配置
 */
@Slf4j
@Configuration
public class RateLimiterConfig {

    /**
     * 默认限流器
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(100, 200);
    }

    /**
     * IP限流器
     */
    @Bean
    public RedisRateLimiter ipRateLimiter() {
        return new RedisRateLimiter(50, 100);
    }

    /**
     * 用户限流器
     */
    @Bean
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(100, 200);
    }

    /**
     * API限流器（针对特定接口）
     */
    @Bean
    public RedisRateLimiter apiRateLimiter() {
        return new RedisRateLimiter(200, 400);
    }

    /**
     * IP地址Key解析器
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * API路径Key解析器
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethodValue();
            return Mono.just("api:" + method + ":" + path);
        };
    }

    /**
     * 用户ID Key解析器（从请求头或Token中提取）
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }
            return Mono.just("user:" + userId);
        };
    }

    /**
     * 复合Key解析器（结合IP和API路径）
     */
    @Bean
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            String path = exchange.getRequest().getPath().value();
            return Mono.just("composite:" + ip + ":" + path);
        };
    }
}
