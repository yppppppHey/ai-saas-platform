package com.aisaas.chat.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 对话服务层 Sentinel 规则（与网关层 SentinelConfig 形成分层限流）：
 *
 * <ul>
 *   <li>网关层：全局/路由 QPS 流控、慢调用与异常比例熔断、系统保护（见 ai-saas-gateway）；</li>
 *   <li>服务层（本类）：核心业务方法 {@code aiChat} 的热点参数(userId)限流，
 *       防止单个用户刷爆对话接口——网关层的路由 QPS 挡不住"合法但高频"的单用户。</li>
 * </ul>
 *
 * <p>差异化限流（普通用户 vs VIP 不同配额）说明：{@link ParamFlowRule} 支持
 * {@code paramFlowItemList} 按具体参数值设置例外阈值，但 userId 是动态数据，
 * 静态写死无意义；生产做法是把规则放到 Nacos 数据源动态下发（VIP 名单变更即生效），
 * 本地仅落地单用户热点限流这一可验证的部分。</p>
 */
@Slf4j
@Configuration
public class SentinelRuleConfig {

    /** 单用户每秒对话请求数上限 */
    private static final double PER_USER_QPS = 10;

    @PostConstruct
    public void initServiceLayerRules() {
        ParamFlowRule hotUserRule = new ParamFlowRule();
        hotUserRule.setResource("aiChat");
        hotUserRule.setParamIdx(0); // chat(Long userId, ...) 第一个参数即用户ID
        hotUserRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        hotUserRule.setCount(PER_USER_QPS);
        hotUserRule.setDurationInSec(1);

        ParamFlowRuleManager.loadRules(List.of(hotUserRule));
        log.info("[Sentinel] 服务层规则加载完成: aiChat 热点参数(userId)限流 {} QPS", PER_USER_QPS);
    }
}
