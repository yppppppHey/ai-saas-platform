package com.aisaas.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * common 层通用 MVC 配置：
 * 注册身份头桥接拦截器（配合 TraceIdFilter 构成完整的
 * "网关 -> 服务" 身份与链路透传）。
 * 各业务服务 scanBasePackages 均包含 com.aisaas.common，自动生效。
 */
@Configuration
public class CommonWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserIdHeaderInterceptor())
                .addPathPatterns("/**");
    }
}
