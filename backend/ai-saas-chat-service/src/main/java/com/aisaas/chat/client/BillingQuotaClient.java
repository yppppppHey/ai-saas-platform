package com.aisaas.chat.client;

import com.aisaas.chat.client.dto.QuotaCheckResultDTO;
import com.aisaas.chat.config.FeignConfig;
import com.aisaas.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 计费服务配额接口客户端
 *
 * <p>name 使用 Nacos 注册的服务名，由 spring-cloud-loadbalancer 完成实例选择，
 * 不需要硬编码 IP；超时与重试在 application.yml 的
 * spring.cloud.openfeign.client.config 下配置。</p>
 *
 * <p>为什么这里用同步调用：发起对话前必须立刻知道「这个用户还能不能用」，
 * 这是强一致读，不能丢进 MQ 慢慢算。而真正扣额度是可异步的写，
 * 交给 MQ + 幂等消费，两条链路职责不同。</p>
 */
@FeignClient(
        name = "ai-saas-billing-service",
        path = "/internal/quota",
        configuration = FeignConfig.class,
        fallbackFactory = BillingQuotaClientFallbackFactory.class
)
public interface BillingQuotaClient {

    /**
     * 同步校验用户配额是否充足
     *
     * @param userId         用户ID
     * @param quotaType      配额类型
     * @param requiredAmount 所需额度
     * @return 校验结果
     */
    @GetMapping("/check")
    Result<QuotaCheckResultDTO> checkQuota(
            @RequestParam("userId") Long userId,
            @RequestParam("quotaType") Integer quotaType,
            @RequestParam("requiredAmount") Long requiredAmount
    );
}
