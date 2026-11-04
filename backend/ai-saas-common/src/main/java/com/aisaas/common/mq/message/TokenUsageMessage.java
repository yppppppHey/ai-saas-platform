package com.aisaas.common.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 用量消息（chat/task -> billing 计费链路）
 *
 * 幂等键为 usageId：由生产方在"业务动作完成后"生成并贯穿全链路，
 * 消费方通过 Redis 标记 + billing_token_usage.uk_usage_id 唯一键双重保证不重复记账。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计费主题 */
    public static final String TOPIC = "token-usage-topic";

    /** 生产方落 Redis 的待对账 hash key（跨服务共享，String 序列化） */
    public static final String PENDING_KEY = "billing:usage:pending";

    /** 消息幂等键（生产方生成，全链路唯一） */
    private String usageId;

    private Long userId;

    private Long conversationId;

    private Long messageId;

    private Long taskId;

    /** 服务商: openai/deepseek */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 操作类型: chat/completion/embedding */
    private String operationType;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    /** 本次调用耗时(毫秒) */
    private Long latencyMs;

    /** 链路追踪ID */
    private String traceId;
}
