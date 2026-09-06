package com.aisaas.chat.mq;

import com.aisaas.common.mq.message.TokenUsageMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Token 用量消息生产者
 *
 * 可靠投递三板斧：
 * 1. 发送前把消息写入 Redis pending hash（对账依据，先记账后发送）
 * 2. syncSend 同步发送（拿到 SendResult 才返回）
 * 3. billing 消费成功后删除 pending；滞留超过阈值的由 billing 定时补偿重放
 *
 * 注意：发送失败只告警不抛出——对话主链路不能被计费拖垮，丢账靠对账补偿兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageProducer {

    private final org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void send(TokenUsageMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("TokenUsageMessage 序列化失败, usageId={}", message.getUsageId(), e);
            return;
        }

        // 1. 先落 pending（宁可多消费被幂等挡掉，不可静默丢账）
        try {
            stringRedisTemplate.opsForHash().put(TokenUsageMessage.PENDING_KEY,
                    message.getUsageId(), System.currentTimeMillis() + "|" + json);
        } catch (Exception e) {
            log.warn("写入 pending 对账记录失败, usageId={}", message.getUsageId(), e);
        }

        // 2. 同步发送，header 带 keys 方便在控制台按 usageId 检索
        try {
            Message<TokenUsageMessage> msg = MessageBuilder.withPayload(message)
                    .setHeader("KEYS", message.getUsageId())
                    .build();
            var result = rocketMQTemplate.syncSend(TokenUsageMessage.TOPIC, msg, 3000);
            log.info("Token用量消息已发送: usageId={}, status={}, totalTokens={}",
                    message.getUsageId(), result.getSendStatus(), message.getTotalTokens());
        } catch (Exception e) {
            // 3. 发送失败不抛出：pending 里留底，billing 补偿任务会重放
            log.error("Token用量消息发送失败, 等待对账补偿, usageId={}", message.getUsageId(), e);
        }
    }
}
