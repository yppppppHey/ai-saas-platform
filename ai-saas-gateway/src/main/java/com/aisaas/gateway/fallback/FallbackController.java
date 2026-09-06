package com.aisaas.gateway.fallback;

import com.aisaas.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * 降级回退控制器
 * 处理服务熔断后的回退响应
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 用户服务降级处理
     */
    @RequestMapping("/user")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> userServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("User service fallback triggered for path: {}", path);
        return Result.error(503, "用户服务暂时不可用，请稍后再试");
    }

    /**
     * 对话服务降级处理
     */
    @RequestMapping("/chat")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> chatServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("Chat service fallback triggered for path: {}", path);
        return Result.error(503, "对话服务暂时不可用，请稍后再试");
    }

    /**
     * 任务服务降级处理
     */
    @RequestMapping("/task")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> taskServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("Task service fallback triggered for path: {}", path);
        return Result.error(503, "任务服务暂时不可用，请稍后再试");
    }

    /**
     * RAG服务降级处理
     */
    @RequestMapping("/rag")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> ragServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("RAG service fallback triggered for path: {}", path);
        return Result.error(503, "知识库服务暂时不可用，请稍后再试");
    }

    /**
     * 计费服务降级处理
     */
    @RequestMapping("/billing")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> billingServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("Billing service fallback triggered for path: {}", path);
        return Result.error(503, "计费服务暂时不可用，请稍后再试");
    }

    /**
     * 管理服务降级处理
     */
    @RequestMapping("/admin")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> adminServiceFallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("Admin service fallback triggered for path: {}", path);
        return Result.error(503, "管理服务暂时不可用，请稍后再试");
    }
}
