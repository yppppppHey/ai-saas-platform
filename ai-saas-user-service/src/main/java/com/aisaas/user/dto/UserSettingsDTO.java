package com.aisaas.user.dto;

import lombok.Data;

/**
 * 用户设置DTO
 */
@Data
public class UserSettingsDTO {

    /**
     * 主题: light/dark/system
     */
    private String theme;

    /**
     * 语言: zh-CN/en-US/ja-JP
     */
    private String language;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 默认模型
     */
    private String defaultModel;

    /**
     * 通知设置(JSON字符串)
     */
    private String notification;

    /**
     * 隐私设置(JSON字符串)
     */
    private String privacy;

    /**
     * 其他设置(JSON字符串)
     */
    private String extraConfig;
}
