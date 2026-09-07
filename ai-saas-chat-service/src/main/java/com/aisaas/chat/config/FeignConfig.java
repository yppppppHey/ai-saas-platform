package com.aisaas.chat.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 通用配置
 *
 * <p>核心职责是把「进程内的请求上下文」翻译成「跨进程能传的 HTTP 头」——
 * 与网关把 X-User-Id 翻译成 MVC attribute 是同一个问题的两个方向：
 * 跨进程只能传头，进程内只能用 attribute，中间必须有人做翻译。</p>
 */
public class FeignConfig {

    /** 与 common UserIdHeaderInterceptor 约定一致 */
    private static final String HEADER_USER_ID = "X-User-Id";
    /** 与 common TraceIdFilter.HEADER 约定一致 */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    /** 与 common TraceIdFilter.MDC_KEY 约定一致 */
    private static final String MDC_TRACE_ID = "traceId";

    /**
     * 透传身份与链路追踪头。
     *
     * <p>没有这个拦截器，下游服务拿不到 userId，链路也会断成两截 traceId。</p>
     */
    @Bean
    public RequestInterceptor feignContextPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader(HEADER_USER_ID);
                if (StringUtils.hasText(userId)) {
                    template.header(HEADER_USER_ID, userId);
                }
            }

            // traceId 可能来自上游请求，也可能由本服务 TraceIdFilter 生成，统一取 MDC
            String traceId = MDC.get(MDC_TRACE_ID);
            if (StringUtils.hasText(traceId)) {
                template.header(HEADER_TRACE_ID, traceId);
            }
        };
    }

    /**
     * 输出完整请求/响应日志（生产建议改用 BASIC 或 NONE，避免打爆磁盘）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
