package com.aisaas.user.service;

import com.aisaas.user.dto.*;
import com.aisaas.common.result.Result;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    Result<TokenDTO> register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    Result<TokenDTO> login(LoginDTO loginDTO);

    /**
     * 用户登出
     */
    Result<Void> logout(String token);

    /**
     * 刷新Token
     */
    Result<TokenDTO> refreshToken(String refreshToken);

    /**
     * 发送验证码
     */
    Result<String> sendVerifyCode(String target, String type);

    /**
     * 验证验证码
     */
    Result<Boolean> verifyCode(String target, String code, String key);

    /**
     * 重置密码
     */
    Result<Void> resetPassword(String email, String newPassword, String verifyCode);
}
