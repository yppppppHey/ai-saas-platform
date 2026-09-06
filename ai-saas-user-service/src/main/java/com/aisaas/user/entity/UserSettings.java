package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户设置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_settings")
public class UserSettings extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

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
    @TableField("default_model")
    private String defaultModel;

    /**
     * 通知设置(JSON)
     */
    private String notification;

    /**
     * 隐私设置(JSON)
     */
    private String privacy;

    /**
     * 其他设置(JSON)
     */
    @TableField("extra_config")
    private String extraConfig;
}
