package com.aisaas.common.mq;

import com.aisaas.common.util.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 消息幂等性处理器
 * 用于防止消息重复消费
 */
@Slf4j
@Component
public class IdempotentMessageHandler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 幂等key默认过期时间（7天）
    private static final long DEFAULT_IDEMPOTENT_TIMEOUT = 7 * 24 * 60 * 60;

    /**
     * 检查消息是否已处理（幂等检查）
     *
     * @param messageId 消息ID
     * @return true表示消息已处理，false表示未处理
     */
    public boolean isMessageProcessed(String messageId) {
        String idempotentKey = RedisKeys.idempotent(messageId);
        Boolean exists = redisTemplate.hasKey(idempotentKey);
        return exists != null && exists;
    }

    /**
     * 标记消息为已处理
     *
     * @param messageId 消息ID
     */
    public void markMessageAsProcessed(String messageId) {
        markMessageAsProcessed(messageId, DEFAULT_IDEMPOTENT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 标记消息为已处理（自定义过期时间）
     *
     * @param messageId   消息ID
     * @param timeout     过期时间
     * @param timeUnit    时间单位
     */
    public void markMessageAsProcessed(String messageId, long timeout, TimeUnit timeUnit) {
        String idempotentKey = RedisKeys.idempotent(messageId);
        redisTemplate.opsForValue().set(idempotentKey, "1", timeout, timeUnit);
        log.debug("Marked message as processed: {}", messageId);
    }

    /**
     * 清除消息处理标记（用于重试场景）
     *
     * @param messageId 消息ID
     */
    public void clearMessageProcessedMark(String messageId) {
        String idempotentKey = RedisKeys.idempotent(messageId);
        redisTemplate.delete(idempotentKey);
        log.debug("Cleared message processed mark: {}", messageId);
    }

    /**
     * 获取消息处理时间（如果已处理）
     *
     * @param messageId 消息ID
     * @return 剩余过期时间（秒），如果不存在则返回-2，如果已过期返回-1
     */
    public Long getMessageTtl(String messageId) {
        String idempotentKey = RedisKeys.idempotent(messageId);
        return redisTemplate.getExpire(idempotentKey, TimeUnit.SECONDS);
    }

    /**
     * 幂等性消费包装器
     * 如果消息已处理则返回true，否则执行消费逻辑并标记为已处理
     *
     * @param messageId 消息ID
     * @param consumer  消费逻辑
     * @return true表示处理成功或已处理，false表示处理失败
     */
    public boolean consumeWithIdempotent(String messageId, java.util.function.Supplier<Boolean> consumer) {
        // 检查是否已处理
        if (isMessageProcessed(messageId)) {
            log.info("Message already processed, skip: {}", messageId);
            return true;
        }

        try {
            // 执行消费逻辑
            Boolean result = consumer.get();

            if (result != null && result) {
                // 标记为已处理
                markMessageAsProcessed(messageId);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to consume message: {}", messageId, e);
            return false;
        }
    }
}
