package com.aisaas.user.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户登录日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_login_log")
public class UserLoginLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 登录方式: 1-密码 2-短信 3-邮箱 4-第三方
     */
    @TableField("login_type")
    private Integer loginType;

    /**
     * 登录IP
     */
    @TableField("login_ip")
    private String loginIp;

    /**
     * 登录地点
     */
    @TableField("login_location")
    private String loginLocation;

    /**
     * 浏览器UA
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 设备标识
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 登录状态: 0-失败 1-成功
     */
    @TableField("login_status")
    private Integer loginStatus;

    /**
     * 失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 登录时间
     */
    @TableField("login_at")
    private LocalDateTime loginAt;

    /**
     * 登出时间
     */
    @TableField("logout_at")
    private LocalDateTime logoutAt;
}
