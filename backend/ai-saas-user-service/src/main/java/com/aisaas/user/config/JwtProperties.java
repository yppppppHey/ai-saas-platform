package com.aisaas.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT配置属性
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥
     */
    private String secret;

    /**
     * 访问令牌过期时间（秒）
     */
    private Long accessExpiration = 86400L;

    /**
     * 刷新令牌过期时间（秒）
     */
    private Long refreshExpiration = 604800L;

    /**
     * 签发者
     */
    private String issuer = "ai-saas-platform";
}
