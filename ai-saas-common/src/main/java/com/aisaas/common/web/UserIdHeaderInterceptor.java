package com.aisaas.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 网关身份头 -> request attribute 桥接
 *
 * 背景：网关 JwtAuthenticationFilter 验签后向下游写入 X-User-Id/X-Username/X-User-Role，
 * 而 chat 等服务的 Controller 统一用 @RequestAttribute("userId") 取身份。
 * user-service 有自己的 JWT 拦截器所以内部是通的，其他服务没人做这一步，
 * 导致 @RequestAttribute 取不到值直接 400——本拦截器补齐这条链路。
 *
 * 信任模型：身份头由网关注入（网关负责剥掉外部伪造的同名头），
 * 服务间内网直连时也允许带头调用（服务间调用的简化处理）。
 * 未带身份头且接口要求 @RequestAttribute 时仍会 400——即"必须经过网关"。
 */
@Slf4j
public class UserIdHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        if (StringUtils.hasText(userId)) {
            try {
                request.setAttribute("userId", Long.parseLong(userId));
            } catch (NumberFormatException e) {
                log.warn("非法的 X-User-Id 头: {}", userId);
            }
        }
        String username = request.getHeader("X-Username");
        if (StringUtils.hasText(username)) {
            request.setAttribute("username", username);
        }
        String role = request.getHeader("X-User-Role");
        if (StringUtils.hasText(role)) {
            request.setAttribute("userRole", role);
        }
        return true;
    }
}
