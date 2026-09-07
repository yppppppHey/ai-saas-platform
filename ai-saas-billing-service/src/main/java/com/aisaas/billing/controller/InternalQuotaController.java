package com.aisaas.billing.controller;

import com.aisaas.billing.dto.QuotaCheckResultVO;
import com.aisaas.billing.service.QuotaService;
import com.aisaas.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部配额接口
 *
 * <p>仅供服务间同步调用（OpenFeign）。网关只路由 /api/v1/** 前缀，
 * 不会转发 /internal/** ，因此该接口对外网不可达，不会被越权探测；
 * 内部调用走 Nacos 服务发现直连 billing-service，不经网关。</p>
 *
 * <p>设计取舍：这里只做「查询/校验」，不做扣减。配额扣减属于可异步的写操作，
 * 走 MQ + 幂等最终一致；而「是否允许发起」属于强一致读，必须同步得到结论，
 * 因此拆成「同步校验 + 异步扣减」两条链路。</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/quota")
public class InternalQuotaController {

    @Autowired
    private QuotaService quotaService;

    /**
     * 配额校验（同步）
     *
     * @param userId         用户ID
     * @param quotaType      配额类型，默认 1
     * @param requiredAmount 需要的额度，默认 1
     * @return 校验结果，available=false 表示不可用并附带原因
     */
    @GetMapping("/check")
    public Result<QuotaCheckResultVO> checkQuota(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "quotaType", defaultValue = "1") Integer quotaType,
            @RequestParam(value = "requiredAmount", defaultValue = "1") Long requiredAmount) {

        Result<QuotaCheckResultVO> result = quotaService.checkQuota(userId, quotaType, requiredAmount);

        QuotaCheckResultVO data = result != null ? result.getData() : null;
        log.info("内部配额校验: userId={}, quotaType={}, required={}, available={}, reason={}",
                userId, quotaType, requiredAmount,
                data != null ? data.getAvailable() : null,
                data != null ? data.getReason() : null);

        return result;
    }
}
