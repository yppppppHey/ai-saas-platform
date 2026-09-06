package com.aisaas.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfig {

    /**
     * JWT配置
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 日志配置
     */
    private LogConfig log = new LogConfig();

    /**
     * 白名单路径（无需认证）
     */
    private List<String> whiteList = new ArrayList<>();

    /**
     * 匿名访问路径
     */
    private List<String> anonymousPaths = new ArrayList<>();

    @Data
    public static class JwtConfig {
        private String secret = "ai-saas-platform-jwt-secret-key-2024";
        private Long accessExpiration = 86400L;
        private Long refreshExpiration = 604800L;
        private String issuer = "ai-saas-platform";
    }

    @Data
    public static class RateLimitConfig {
        private boolean enabled = true;
        private int defaultReplenishRate = 100;
        private int defaultBurstCapacity = 200;
        private int ipLimit = 50;
        private int userLimit = 100;
        private int apiLimit = 200;
    }

    @Data
    public static class SecurityConfig {
        private boolean xssProtection = true;
        private boolean sqlInjectionProtection = true;
        private boolean ipBlacklistEnabled = true;
        private boolean ipWhitelistEnabled = false;
        private List<String> ipBlacklist = new ArrayList<>();
        private List<String> ipWhitelist = new ArrayList<>();
    }

    @Data
    public static class LogConfig {
        private boolean accessLogEnabled = true;
        private boolean requestLogEnabled = true;
        private boolean responseLogEnabled = true;
        private boolean performanceLogEnabled = true;
        private long slowRequestThreshold = 1000L;
    }
}
