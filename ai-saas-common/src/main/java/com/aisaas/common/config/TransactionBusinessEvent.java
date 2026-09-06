package com.aisaas.common.config;

import org.springframework.context.ApplicationEvent;

/**
 * 事务消息本地业务逻辑事件。
 *
 * <p>当 {@link RocketMQTransactionConfig#executeLocalBusiness(String, String)} 被调用时，
 * 框架会发布本事件，由业务侧通过 {@code @EventListener(TransactionBusinessEvent.class)}
 * 订阅并执行真正的本地事务（如写库、扣减库存等）。由于 Spring 默认使用同步事件多播器，
 * 监听器会在 {@code executeLocalBusiness} 返回前执行；若监听器抛出异常，则本地事务判定为失败，
 * 触发 RocketMQ 半消息回滚。</p>
 */
public class TransactionBusinessEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /** 事务ID（与 RocketMQ 事务ID对应） */
    private final String transactionId;

    /** 业务负载（通常为 JSON 字符串，由订阅方自行反序列化） */
    private final String payload;

    public TransactionBusinessEvent(String transactionId, String payload) {
        super(transactionId);
        this.transactionId = transactionId;
        this.payload = payload;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "TransactionBusinessEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
