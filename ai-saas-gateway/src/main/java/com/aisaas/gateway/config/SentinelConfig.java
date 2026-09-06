package com.aisaas.gateway.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Sentinel熔断降级配置
 */
@Slf4j
@Configuration
public class SentinelConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                          ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 配置Sentinel网关过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 配置限流降级异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer) {
            @Override
            public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
                if (exchange.getResponse().isCommitted()) {
                    return Mono.error(ex);
                }

                String message;
                int statusCode = HttpStatus.TOO_MANY_REQUESTS.value();

                if (ex instanceof BlockException) {
                    message = "请求过于频繁，请稍后再试";
                } else if (ex instanceof DegradeException) {
                    message = "服务暂时不可用，请稍后再试";
                    statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
                } else if (ex instanceof ParamFlowException) {
                    message = "热点参数限流触发";
                } else if (ex instanceof SystemBlockException) {
                    message = "系统负载过高，请稍后再试";
                    statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
                } else {
                    message = "访问被拒绝";
                }

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", statusCode);
                errorResponse.put("message", message);
                errorResponse.put("data", null);
                errorResponse.put("timestamp", System.currentTimeMillis());

                return ServerResponse.status(statusCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(errorResponse))
                        .flatMap(response -> response.writeTo(exchange, new ServerResponse.Context() {
                            @Override
                            public List<HttpMessageWriter<?>> messageWriters() {
                                return serverCodecConfigurer.getWriters();
                            }

                            @Override
                            public List<ViewResolver> viewResolvers() {
                                return viewResolvers;
                            }
                        }));
            }
        };
    }

    /**
     * 初始化Sentinel规则
     */
    @PostConstruct
    public void initSentinelRules() {
        initFlowRules();
        initDegradeRules();
        initParamFlowRules();
        initSystemRules();
        log.info("Sentinel rules initialized successfully");
    }

    /**
     * 初始化流控规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 全局流控规则
        FlowRule globalRule = new FlowRule();
        globalRule.setResource("__default__");
        globalRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        globalRule.setCount(1000);
        globalRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        globalRule.setWarmUpPeriodSec(10);
        rules.add(globalRule);

        // 用户服务流控
        FlowRule userServiceRule = new FlowRule();
        userServiceRule.setResource("ai-saas-user-service");
        userServiceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        userServiceRule.setCount(200);
        rules.add(userServiceRule);

        // 对话服务流控（重点保护）
        FlowRule chatServiceRule = new FlowRule();
        chatServiceRule.setResource("ai-saas-chat-service");
        chatServiceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        chatServiceRule.setCount(100);
        chatServiceRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(chatServiceRule);

        // 任务服务流控
        FlowRule taskServiceRule = new FlowRule();
        taskServiceRule.setResource("ai-saas-task-service");
        taskServiceRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        taskServiceRule.setCount(50);
        rules.add(taskServiceRule);

        FlowRuleManager.loadRules(rules);
        log.info("Flow rules loaded: {}", rules.size());
    }

    /**
     * 初始化熔断降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 对话服务熔断规则（慢调用比例）
        DegradeRule chatSlowCallRule = new DegradeRule();
        chatSlowCallRule.setResource("ai-saas-chat-service");
        chatSlowCallRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        chatSlowCallRule.setCount(500); // 慢调用阈值500ms
        chatSlowCallRule.setSlowRatioThreshold(0.5); // 慢调用比例阈值50%
        chatSlowCallRule.setTimeWindow(30); // 熔断时长30秒
        chatSlowCallRule.setMinRequestAmount(10); // 最小请求数
        chatSlowCallRule.setStatIntervalMs(1000); // 统计时长1秒
        rules.add(chatSlowCallRule);

        // 对话服务熔断规则（异常比例）
        DegradeRule chatErrorRule = new DegradeRule();
        chatErrorRule.setResource("ai-saas-chat-service");
        chatErrorRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        chatErrorRule.setCount(0.5); // 异常比例阈值50%
        chatErrorRule.setTimeWindow(30);
        chatErrorRule.setMinRequestAmount(10);
        chatErrorRule.setStatIntervalMs(1000);
        rules.add(chatErrorRule);

        // 用户服务熔断规则
        DegradeRule userServiceRule = new DegradeRule();
        userServiceRule.setResource("ai-saas-user-service");
        userServiceRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        userServiceRule.setCount(0.7);
        userServiceRule.setTimeWindow(20);
        userServiceRule.setMinRequestAmount(5);
        rules.add(userServiceRule);

        // 任务服务熔断规则
        DegradeRule taskServiceRule = new DegradeRule();
        taskServiceRule.setResource("ai-saas-task-service");
        taskServiceRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        taskServiceRule.setCount(3000); // 任务服务容忍3秒慢调用
        taskServiceRule.setSlowRatioThreshold(0.6);
        taskServiceRule.setTimeWindow(60);
        taskServiceRule.setMinRequestAmount(5);
        rules.add(taskServiceRule);

        DegradeRuleManager.loadRules(rules);
        log.info("Degrade rules loaded: {}", rules.size());
    }

    /**
     * 初始化热点参数限流规则
     */
    private void initParamFlowRules() {
        List<ParamFlowRule> rules = new ArrayList<>();

        // 对话接口热点参数限流（按用户ID）
        ParamFlowRule chatUserRule = new ParamFlowRule();
        chatUserRule.setResource("/api/v1/chat/completions");
        chatUserRule.setParamIdx(0); // 第一个参数（用户ID）
        chatUserRule.setCount(10); // 每用户每秒10个请求
        chatUserRule.setDurationInSec(1);
        chatUserRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rules.add(chatUserRule);

        // 任务提交热点参数限流
        ParamFlowRule taskUserRule = new ParamFlowRule();
        taskUserRule.setResource("/api/v1/tasks");
        taskUserRule.setParamIdx(0);
        taskUserRule.setCount(5); // 每用户每秒5个任务
        taskUserRule.setDurationInSec(1);
        rules.add(taskUserRule);

        ParamFlowRuleManager.loadRules(rules);
        log.info("Param flow rules loaded: {}", rules.size());
    }

    /**
     * 初始化系统保护规则
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        SystemRule rule = new SystemRule();
        rule.setHighestSystemLoad(10.0); // 最大系统负载
        rule.setHighestCpuUsage(0.8); // 最大CPU使用率80%
        rule.setAvgRt(1000); // 平均响应时间1秒
        rule.setMaxThread(500); // 最大线程数
        rule.setQps(2000); // 系统级QPS限制
        rules.add(rule);

        SystemRuleManager.loadRules(rules);
        log.info("System rules loaded: {}", rules.size());
    }
}
