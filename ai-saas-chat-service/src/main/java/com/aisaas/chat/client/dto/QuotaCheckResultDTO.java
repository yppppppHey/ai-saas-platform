package com.aisaas.chat.client.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配额校验结果（消费端视图）
 *
 * <p>注意：这里没有直接复用 billing 的 QuotaCheckResultVO。消费者只声明自己
 * 关心的字段，服务方后续给 VO 加字段不会破坏本服务反序列化，避免「服务方内部
 * 模型泄漏到消费方」造成的隐式契约耦合。这也是 Feign/HTTP 调用与直接依赖 jar
 * 相比的一个好处：契约以 JSON 为准，双方可以各自演进。</p>
 */
@Data
public class QuotaCheckResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 配额类型 */
    private Integer quotaType;

    /** 是否可用：false 表示配额不足，不应继续 */
    private Boolean available;

    /** 不可用原因 */
    private String reason;

    /** 每日限额 / 已用 */
    private Long dailyLimit;
    private Long dailyUsed;

    /** 每月限额 / 已用 */
    private Long monthlyLimit;
    private Long monthlyUsed;
}
