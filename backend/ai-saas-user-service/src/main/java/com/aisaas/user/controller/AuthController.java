package com.aisaas.user.controller;

import com.aisaas.common.result.Result;
import com.aisaas.user.dto.*;
import com.aisaas.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<TokenDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authService.logout(token);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenDTO> refreshToken(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<String> sendVerifyCode(@RequestParam String target, 
                                          @RequestParam String type) {
        return authService.sendVerifyCode(target, type);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam String email,
                                       @RequestParam String newPassword,
                                       @RequestParam String verifyCode) {
        return authService.resetPassword(email, newPassword, verifyCode);
    }
}
