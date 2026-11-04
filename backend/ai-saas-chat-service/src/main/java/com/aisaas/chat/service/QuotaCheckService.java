package com.aisaas.chat.service;

import com.aisaas.chat.client.BillingQuotaClient;
import com.aisaas.chat.client.BillingQuotaClientFallbackFactory;
import com.aisaas.chat.client.dto.QuotaCheckResultDTO;
import com.aisaas.common.exception.BizException;
import com.aisaas.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 配额校验（同步链路）
 *
 * <p>把 Feign 调用、降级结果识别、异常兜底收敛到一个业务语义方法里，
 * 调用方只需要一句 {@code checkBeforeChat(userId)}，不用关心 HTTP 细节。</p>
 *
 * <p>链路分工：
 * <ul>
 *   <li><b>同步（OpenFeign）</b>：发起对话前校验「还能不能用」——强一致读，必须立刻有结论</li>
 *   <li><b>异步（MQ + 幂等）</b>：真正扣额度、记用量——可异步的写，最终一致</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaCheckService {

    /** 配额类型：1-对话次数（与 billing 侧约定保持一致） */
    private static final Integer QUOTA_TYPE_CHAT = 1;
    /** 单次对话默认消耗额度 */
    private static final Long DEFAULT_REQUIRED_AMOUNT = 1L;

    private final BillingQuotaClient billingQuotaClient;

    /**
     * 发起对话前校验配额，不足则抛业务异常阻断。
     *
     * <p>异常语义区分：
     * <ul>
     *   <li>配额不足 → {@link BizException}，必须透传，拒绝本次请求</li>
     *   <li>调用异常/降级 → 按 fail-open 放行，只记日志，不阻断主流程</li>
     * </ul>
     * </p>
     *
     * @param userId 用户ID
     */
    public void checkBeforeChat(Long userId) {
        try {
            Result<QuotaCheckResultDTO> result =
                    billingQuotaClient.checkQuota(userId, QUOTA_TYPE_CHAT, DEFAULT_REQUIRED_AMOUNT);

            if (result == null || result.getData() == null) {
                // 无有效结论时按降级策略放行，避免误伤
                log.warn("配额校验无有效返回，按降级策略放行: userId={}", userId);
                return;
            }

            QuotaCheckResultDTO data = result.getData();

            if (Boolean.FALSE.equals(data.getAvailable())) {
                String reason = StringUtils.hasText(data.getReason()) ? data.getReason() : "配额不足，请升级套餐";
                log.info("配额校验未通过: userId={}, reason={}", userId, reason);
                throw new BizException(reason);
            }

            if (BillingQuotaClientFallbackFactory.REASON_FALLBACK.equals(data.getReason())) {
                log.warn("配额校验走降级放行: userId={}", userId);
            }
        } catch (BizException e) {
            // 业务拒绝，原样抛出
            throw e;
        } catch (Exception e) {
            // 未预期异常同样按 fail-open 处理，与 FallbackFactory 策略保持一致
            log.error("配额校验异常，按降级策略放行: userId={}", userId, e);
        }
    }
}
