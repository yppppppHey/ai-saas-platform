package com.aisaas.user.interceptor;

import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.exception.BizException;
import com.aisaas.common.util.RedisKeys;
import com.aisaas.common.util.RedisUtils;
import com.aisaas.user.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisUtils redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的Authorization
        String authorization = request.getHeader("Authorization");

        // 如果没有Authorization头，放行（可能是不需要认证的接口）
        if (!StringUtils.hasText(authorization)) {
            return true;
        }

        // 提取Token
        String token = extractToken(authorization);
        if (!StringUtils.hasText(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "Token格式错误");
        }

        // 检查Token是否在黑名单中
        String blacklistKey = RedisKeys.TOKEN_BLACKLIST + token;
        if (redisUtils.hasKey(blacklistKey)) {
            throw new BizException(ResultCode.TOKEN_INVALID, "Token已失效");
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            throw new BizException(ResultCode.TOKEN_EXPIRED, "Token已过期");
        }

        // 解析Token获取用户信息
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            throw new BizException(ResultCode.TOKEN_INVALID, "Token解析失败");
        }

        Long userId = Long.valueOf(claims.get("userId").toString());
        String username = claims.get("username", String.class);

        // 将用户信息存入request属性中，供后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("token", token);

        log.debug("JWT认证通过: userId={}, username={}", userId, username);
        return true;
    }

    /**
     * 从Authorization头中提取Token
     */
    private String extractToken(String authorization) {
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
