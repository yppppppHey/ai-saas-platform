package com.aisaas.billing.consumer;

import com.aisaas.billing.dto.TokenUsageRecordDTO;
import com.aisaas.billing.service.TokenUsageService;
import com.aisaas.common.mq.IdempotentMessageHandler;
import com.aisaas.common.mq.message.TokenUsageMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Token 用量消息消费者
 *
 * 消费语义：
 * - 幂等：Redis 标记(IdempotentMessageHandler) + DB usage_id 唯一键双保险，
 *         重复投递/并发消费/Redis 标记过期三种场景都不会重复记账
 * - 补偿：消费成功后删除 chat 侧落下的 pending 对账记录；
 *         滞留未删的（发送丢失/重试耗尽）由 UsageReconcileTask 定时重放
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TokenUsageMessage.TOPIC,
        consumerGroup = "billing-usage-consumer-group",
        maxReconsumeTimes = 16
)
public class TokenUsageConsumer implements RocketMQListener<TokenUsageMessage> {

    private final IdempotentMessageHandler idempotentMessageHandler;
    private final TokenUsageService tokenUsageService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(TokenUsageMessage message) {
        String usageId = message.getUsageId();

        boolean ok = idempotentMessageHandler.consumeWithIdempotent(usageId, () -> {
            TokenUsageRecordDTO dto = TokenUsageRecordDTO.builder()
                    .userId(message.getUserId())
                    .conversationId(message.getConversationId())
                    .messageId(message.getMessageId())
                    .taskId(message.getTaskId())
                    .provider(message.getProvider())
                    .modelId(message.getModelId())
                    .operationType(message.getOperationType())
                    .promptTokens(message.getPromptTokens())
                    .completionTokens(message.getCompletionTokens())
                    .totalTokens(message.getTotalTokens())
                    .latencyMs(message.getLatencyMs() == null ? null
                            : message.getLatencyMs().intValue())
                    .traceId(message.getTraceId())
                    .build();

            // DB 级幂等（Redis 标记过期后的兜底），冲突视为成功
            var result = tokenUsageService.recordTokenUsageIdempotent(dto, usageId);
            return result != null && result.isSuccess();
        });

        if (ok) {
            // 记账成功，清掉对账 pending
            try {
                stringRedisTemplate.opsForHash().delete(TokenUsageMessage.PENDING_KEY, usageId);
            } catch (Exception e) {
                log.warn("清理 pending 失败(不影响入账): usageId={}", usageId, e);
            }
        } else {
            // 抛出异常触发 RocketMQ 重试（16 次后进 DLQ，仍有 pending 对账兜底）
            throw new IllegalStateException("Token用量消息消费失败, 等待重试: usageId=" + usageId);
        }
    }
}
