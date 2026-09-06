package com.aisaas.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 死信队列处理器
 * 处理消费失败的消息，支持重试和持久化
 */
@Slf4j
@Component
public class DeadLetterQueueHandler {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${mq.dead-letter.topic:DLQ}")
    private String deadLetterTopic;

    @Value("${mq.dead-letter.max-retries:3}")
    private int maxRetries;

    @Value("${mq.dead-letter.retry-interval-seconds:60}")
    private int retryIntervalSeconds;

    // Redis key前缀
    private static final String DLQ_KEY_PREFIX = "ai:platform:dlq:";
    private static final String DLQ_STATS_KEY = "ai:platform:dlq:stats";

    @PostConstruct
    public void init() {
        log.info("DeadLetterQueueHandler initialized with topic: {}, maxRetries: {}",
                deadLetterTopic, maxRetries);
    }

    /**
     * 将消息发送到死信队列
     *
     * @param message     原始消息
     * @param reason      进入死信队列的原因
     * @param exception   异常信息
     * @param <T>         消息类型
     */
    public <T> void sendToDeadLetter(Message<T> message, String reason, Throwable exception) {
        try {
            // 构建死信消息
            DeadLetterMessage<T> deadLetterMessage = buildDeadLetterMessage(message, reason, exception);

            // 保存到Redis用于追踪
            saveToRedis(deadLetterMessage);

            // 更新统计信息
            updateStats(deadLetterMessage, "sent");

            // 发送到RocketMQ死信队列
            org.springframework.messaging.Message<DeadLetterMessage<T>> mqMessage =
                    MessageBuilder.withPayload(deadLetterMessage)
                            .setHeader("KEYS", deadLetterMessage.getMessageId())
                            .setHeader("MSG_ID", deadLetterMessage.getMessageId())
                            .build();

            rocketMQTemplate.syncSend(deadLetterTopic, mqMessage);

            log.info("Message sent to dead letter queue: {}, reason: {}",
                    message.getMessageId(), reason);

        } catch (Exception e) {
            log.error("Failed to send message to dead letter queue: {}", message.getMessageId(), e);
        }
    }

    /**
     * 尝试重新处理死信消息
     *
     * @param messageId   消息ID
     * @param retryHandler 重试处理逻辑
     * @param <T>         消息类型
     * @return 重试是否成功
     */
    public <T> boolean retryDeadLetter(String messageId, RetryHandler<T> retryHandler) {
        String redisKey = DLQ_KEY_PREFIX + messageId;

        try {
            @SuppressWarnings("unchecked")
            DeadLetterMessage<T> deadLetterMessage = (DeadLetterMessage<T>) redisTemplate.opsForValue().get(redisKey);

            if (deadLetterMessage == null) {
                log.warn("Dead letter message not found: {}", messageId);
                return false;
            }

            if (deadLetterMessage.getRetryCount() >= maxRetries) {
                log.warn("Max retries reached for message: {}", messageId);
                deadLetterMessage.setStatus(DeadLetterStatus.PERMANENT_FAIL);
                updateStats(deadLetterMessage, "permanent_fail");
                return false;
            }

            // 执行重试
            boolean success = retryHandler.retry(deadLetterMessage.getOriginalMessage());

            if (success) {
                deadLetterMessage.setStatus(DeadLetterStatus.RETRY_SUCCESS);
                updateStats(deadLetterMessage, "retry_success");
                redisTemplate.delete(redisKey);
                log.info("Successfully retried dead letter message: {}", messageId);
            } else {
                deadLetterMessage.incrementRetryCount();
                deadLetterMessage.setStatus(DeadLetterStatus.RETRYING);
                redisTemplate.opsForValue().set(redisKey, deadLetterMessage);
                updateStats(deadLetterMessage, "retry_fail");
                log.warn("Retry failed for message: {}, count: {}", messageId, deadLetterMessage.getRetryCount());
            }

            return success;

        } catch (Exception e) {
            log.error("Failed to retry dead letter message: {}", messageId, e);
            return false;
        }
    }

    /**
     * 获取死信消息统计
     *
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(DLQ_STATS_KEY);
        Map<String, Object> stats = new HashMap<>();
        entries.forEach((k, v) -> stats.put(k.toString(), v));
        return stats;
    }

    // ============ 私有方法 ============

    private <T> DeadLetterMessage<T> buildDeadLetterMessage(Message<T> message, String reason, Throwable exception) {
        return DeadLetterMessage.<T>builder()
                .messageId(message.getMessageId() != null ? message.getMessageId() : UUID.randomUUID().toString())
                .originalMessage(message)
                .reason(reason)
                .exceptionMessage(exception != null ? exception.getMessage() : null)
                .exceptionStackTrace(getStackTraceString(exception))
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .maxRetryCount(maxRetries)
                .status(DeadLetterStatus.NEW)
                .build();
    }

    private <T> void saveToRedis(DeadLetterMessage<T> deadLetterMessage) {
        String redisKey = DLQ_KEY_PREFIX + deadLetterMessage.getMessageId();
        redisTemplate.opsForValue().set(redisKey, deadLetterMessage);
        // 设置过期时间（30天）
        redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
    }

    private <T> void updateStats(DeadLetterMessage<T> message, String event) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
        String key = "ai:platform:dlq:stats:" + today;

        redisTemplate.opsForHash().increment(key, event, 1);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);

        // 更新全局统计
        redisTemplate.opsForHash().increment(DLQ_STATS_KEY, event, 1);
    }

    private String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    // ============ 内部类和接口 ============

    /**
     * 死信消息状态枚举
     */
    public enum DeadLetterStatus {
        NEW,              // 新建
        RETRYING,         // 重试中
        RETRY_SUCCESS,    // 重试成功
        PERMANENT_FAIL    // 永久失败（超过最大重试次数）
    }

    /**
     * 死信消息封装
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeadLetterMessage<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        private String messageId;
        private Message<T> originalMessage;
        private String reason;
        private String exceptionMessage;
        private String exceptionStackTrace;
        private LocalDateTime createTime;
        private Integer retryCount;
        private Integer maxRetryCount;
        private DeadLetterStatus status;

        public void incrementRetryCount() {
            if (this.retryCount == null) {
                this.retryCount = 0;
            }
            this.retryCount++;
        }
    }

    /**
     * 重试处理器接口
     */
    @FunctionalInterface
    public interface RetryHandler<T> {
        boolean retry(Message<T> message);
    }
}
