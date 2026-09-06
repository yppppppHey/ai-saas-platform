package com.aisaas.billing.schedule;

import com.aisaas.billing.dto.TokenUsageRecordDTO;
import com.aisaas.billing.service.TokenUsageService;
import com.aisaas.common.mq.message.TokenUsageMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Token 用量对账补偿任务
 *
 * 背景：chat 发送计费消息前会把消息写入 Redis pending hash
 * （billing:usage:pending，field=usageId，value=发送时间|消息JSON），
 * billing 消费成功后删除对应 field。
 *
 * 本任务每分钟扫描 pending：
 * - 滞留超过 10 分钟 = "发送丢失"或"消费重试耗尽"仍未入账
 * - 反序列化后走同一套幂等消费逻辑重放（重放命中 DB 唯一键也不会重复记账）
 * - 重放成功或确认已入账后删除 pending
 *
 * 这条链路保证了"对话完成 → 计费入账"的最终一致性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageReconcileTask {

    /** pending 滞留超过 10 分钟才补偿，避免与正常消费竞争 */
    private static final long PENDING_TIMEOUT_MS = 10 * 60 * 1000L;

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenUsageService tokenUsageService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void reconcile() {
        Map<Object, Object> pending = stringRedisTemplate.opsForHash()
                .entries(TokenUsageMessage.PENDING_KEY);
        if (pending.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int replayed = 0;
        int dropped = 0;

        for (Map.Entry<Object, Object> entry : pending.entrySet()) {
            String usageId = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());

            int sep = value.indexOf('|');
            if (sep <= 0) {
                // 脏数据，直接清掉
                stringRedisTemplate.opsForHash().delete(TokenUsageMessage.PENDING_KEY, usageId);
                dropped++;
                continue;
            }

            long sendTime;
            String json;
            try {
                sendTime = Long.parseLong(value.substring(0, sep));
                json = value.substring(sep + 1);
            } catch (Exception e) {
                stringRedisTemplate.opsForHash().delete(TokenUsageMessage.PENDING_KEY, usageId);
                dropped++;
                continue;
            }

            if (now - sendTime < PENDING_TIMEOUT_MS) {
                continue; // 还在正常消费窗口内
            }

            try {
                TokenUsageMessage message = objectMapper.readValue(json, TokenUsageMessage.class);
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
                        .traceId(message.getTraceId())
                        .build();

                var result = tokenUsageService.recordTokenUsageIdempotent(dto, usageId);
                boolean success = result != null && result.isSuccess();
                if (success) {
                    // 入账成功（或幂等命中 = 早已入账），清理 pending
                    stringRedisTemplate.opsForHash().delete(TokenUsageMessage.PENDING_KEY, usageId);
                    replayed++;
                    log.info("对账补偿完成: usageId={}", usageId);
                } else {
                    log.warn("对账补偿入账失败, 保留 pending 下轮重试: usageId={}", usageId);
                }
            } catch (Exception e) {
                log.error("对账补偿异常, 保留 pending 下轮重试: usageId={}", usageId, e);
            }
        }

        if (replayed > 0 || dropped > 0) {
            log.info("对账任务执行完毕: 补偿入账={} 条, 清理脏数据={} 条", replayed, dropped);
        }
    }
}
