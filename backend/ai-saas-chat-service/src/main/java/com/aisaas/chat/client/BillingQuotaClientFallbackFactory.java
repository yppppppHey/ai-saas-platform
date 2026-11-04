package com.aisaas.chat.client;

import com.aisaas.chat.client.dto.QuotaCheckResultDTO;
import com.aisaas.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 配额校验降级实现
 *
 * <p><b>降级策略：fail-open（放行）。</b></p>
 *
 * <p>权衡过程：配额校验对对话链路而言是「保护性依赖」，不是产生业务结果的必需输入。
 * 若选择 fail-closed（计费服务不可用就拒绝服务），billing 一旦抖动，整个对话功能
 * 立刻不可用——可用性损失远大于少量超额的风险。因此选择放行，同时：</p>
 * <ul>
 *   <li>打 error 日志并带上原因，便于监控告警</li>
 *   <li>超额部分由异步对账任务（UsageReconcileTask）+ 配额扣减兜底，不会真的白嫖</li>
 * </ul>
 *
 * <p>反过来，如果是「扣款」「下单」这类核心写操作，就应该 fail-closed 并
 * 返回明确失败，绝不能静默放行。降级策略要按业务语义选，不能一刀切。</p>
 */
@Slf4j
@Component
public class BillingQuotaClientFallbackFactory implements FallbackFactory<BillingQuotaClient> {

    /** 降级标记，写入 reason 便于链路追踪时识别 */
    public static final String REASON_FALLBACK = "billing-unavailable-fail-open";

    @Override
    public BillingQuotaClient create(Throwable cause) {
        return (userId, quotaType, requiredAmount) -> {
            log.error("配额校验降级(fail-open): userId={}, quotaType={}, required={}, cause={}",
                    userId, quotaType, requiredAmount, cause.toString());

            QuotaCheckResultDTO degraded = new QuotaCheckResultDTO();
            degraded.setUserId(userId);
            degraded.setQuotaType(quotaType);
            degraded.setAvailable(true);
            degraded.setReason(REASON_FALLBACK);
            return Result.success(degraded);
        };
    }
}
