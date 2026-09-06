package com.aisaas.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginDTO {

    /**
     * 用户名/邮箱/手机号
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码(可选)
     */
    private String captcha;

    /**
     * 验证码key
     */
    private String captchaKey;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 记住我
     */
    private Boolean rememberMe = false;
}
