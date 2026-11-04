package com.aisaas.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 统一消息体格式
 * 用于消息队列的标准消息封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID（用于幂等性校验）
     */
    private String messageId;

    /**
     * 消息版本号
     */
    private Integer version;

    /**
     * 消息类型（业务类型）
     */
    private String messageType;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tags;

    /**
     * 消息Keys（用于查询）
     */
    private String keys;

    /**
     * 消息体（业务数据）
     */
    private T payload;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 消息来源（服务名）
     */
    private String source;

    /**
     * 目标服务
     */
    private String target;

    /**
     * 扩展属性
     */
    private Map<String, String> properties;

    /**
     * 消息状态
     */
    private MessageStatus status;

    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING,      // 待发送
        SENDING,      // 发送中
        SENT,         // 已发送
        DELIVERED,    // 已投递
        CONSUMED,     // 已消费
        FAILED,       // 发送失败
        RETRYING,     // 重试中
        DEAD_LETTER   // 死信
    }

    /**
     * 创建消息（简化版）
     */
    public static <T> Message<T> create(String messageType, T payload) {
        return Message.<T>builder()
                .messageId(UUID.randomUUID().toString().replace("-", ""))
                .version(1)
                .messageType(messageType)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .maxRetryCount(3)
                .status(MessageStatus.PENDING)
                .build();
    }

    /**
     * 创建消息（完整版）
     */
    public static <T> Message<T> create(String topic, String tags, String messageType, T payload) {
        return Message.<T>builder()
                .messageId(UUID.randomUUID().toString().replace("-", ""))
                .version(1)
                .topic(topic)
                .tags(tags)
                .messageType(messageType)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .maxRetryCount(3)
                .status(MessageStatus.PENDING)
                .build();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
        this.status = MessageStatus.RETRYING;
    }

    /**
     * 标记为死信
     */
    public void markAsDeadLetter() {
        this.status = MessageStatus.DEAD_LETTER;
    }

    /**
     * 是否达到最大重试次数
     */
    public boolean isMaxRetryReached() {
        return this.retryCount != null && this.maxRetryCount != null
                && this.retryCount >= this.maxRetryCount;
    }
}
