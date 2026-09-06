package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户账户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_account")
public class UserAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码哈希(BCrypt)
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 用户类型: 1-普通用户 2-VIP用户 9-管理员
     */
    @TableField("user_type")
    private Integer userType;

    /**
     * 状态: 0-禁用 1-正常 2-待验证
     */
    private Integer status;

    @TableField("vip_level")
    private Integer vipLevel;

    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 用户设置(JSON)
     */
    private String settings;
}
