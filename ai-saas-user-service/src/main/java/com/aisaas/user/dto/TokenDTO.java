package com.aisaas.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token信息DTO
 */
@Data
public class TokenDTO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间(秒)
     */
    private Long expiresIn;

    /**
     * 令牌过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 刷新令牌过期时间
     */
    private LocalDateTime refreshExpireTime;
}
