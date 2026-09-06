package com.aisaas.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ 事务消息配置
 * 实现事务消息支持配置
 */
@Slf4j
@Configuration
public class RocketMQTransactionConfig {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 应用事件发布器：用于在本地事务阶段把业务负载交给业务侧处理
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    // 事务状态记录（实际项目中应该使用Redis或数据库）
    private static final Map<String, TransactionRecord> transactionRecords = new ConcurrentHashMap<>();

    // 事务状态枚举
    public enum TransactionStatus {
        UNKNOWN,    // 未知状态
        COMMIT,     // 提交
        ROLLBACK    // 回滚
    }

    /**
     * 事务记录
     */
    public static class TransactionRecord {
        private final String transactionId;
        private TransactionStatus status;
        private final long createTime;
        private long updateTime;
        private int checkCount;
        private String payload;

        public TransactionRecord(String transactionId, String payload) {
            this.transactionId = transactionId;
            this.payload = payload;
            this.status = TransactionStatus.UNKNOWN;
            this.createTime = System.currentTimeMillis();
            this.updateTime = this.createTime;
            this.checkCount = 0;
        }

        public void updateStatus(TransactionStatus status) {
            this.status = status;
            this.updateTime = System.currentTimeMillis();
        }

        public void incrementCheckCount() {
            this.checkCount++;
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public TransactionStatus getStatus() { return status; }
        public long getCreateTime() { return createTime; }
        public long getUpdateTime() { return updateTime; }
        public int getCheckCount() { return checkCount; }
        public String getPayload() { return payload; }
    }

    /**
     * 事务监听器
     */
    @Bean
    public TransactionListener transactionListener() {
        return new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                String transactionId = msg.getTransactionId();
                String payload = new String(msg.getBody());

                log.info("Executing local transaction: {}", transactionId);

                try {
                    // 记录事务
                    TransactionRecord record = new TransactionRecord(transactionId, payload);
                    transactionRecords.put(transactionId, record);

                    // 执行本地业务逻辑
                    boolean success = executeLocalBusiness(transactionId, payload);

                    if (success) {
                        record.updateStatus(TransactionStatus.COMMIT);
                        log.info("Local transaction committed: {}", transactionId);
                        return LocalTransactionState.COMMIT_MESSAGE;
                    } else {
                        record.updateStatus(TransactionStatus.ROLLBACK);
                        log.warn("Local transaction rolled back: {}", transactionId);
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }

                } catch (Exception e) {
                    log.error("Local transaction failed: {}", transactionId, e);
                    TransactionRecord record = transactionRecords.get(transactionId);
                    if (record != null) {
                        record.updateStatus(TransactionStatus.ROLLBACK);
                    }
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String transactionId = msg.getTransactionId();
                TransactionRecord record = transactionRecords.get(transactionId);

                if (record == null) {
                    log.warn("Transaction record not found: {}", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }

                record.incrementCheckCount();
                log.info("Checking local transaction: {}, status: {}, check count: {}",
                        transactionId, record.getStatus(), record.getCheckCount());

                switch (record.getStatus()) {
                    case COMMIT:
                        return LocalTransactionState.COMMIT_MESSAGE;
                    case ROLLBACK:
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    case UNKNOWN:
                    default:
                        // 如果检查次数过多，可能是业务执行出现问题，建议回滚
                        if (record.getCheckCount() > 10) {
                            log.warn("Too many checks for transaction: {}, rolling back", transactionId);
                            return LocalTransactionState.ROLLBACK_MESSAGE;
                        }
                        return LocalTransactionState.UNKNOW;
                }
            }
        };
    }

    /**
     * 执行本地业务逻辑。
     *
     * <p>本地事务阶段的核心实现：通过 {@link ApplicationEventPublisher} 发布
     * {@link TransactionBusinessEvent}，由业务侧使用 {@code @EventListener} 订阅并执行真实的
     * 本地事务（写库、扣减库存等）。由于 Spring 默认使用同步事件多播器，监听器会在此方法返回前
     * 完成执行；若任一监听器抛出异常，则判定本地事务失败，进而触发 RocketMQ 半消息回滚。</p>
     *
     * <p>若容器中不存在 {@link ApplicationEventPublisher}（极少见，非 Spring 环境），
     * 则仅记录日志并返回 {@code true}，保证不破坏既有调用方行为。</p>
     *
     * @param transactionId 事务ID
     * @param payload       业务数据（通常为 JSON 字符串）
     * @return 业务执行是否成功
     */
    private boolean executeLocalBusiness(String transactionId, String payload) {
        log.info("Executing local business logic for transaction: {}", transactionId);

        if (eventPublisher == null) {
            log.warn("No ApplicationEventPublisher available, skip local business for transaction: {}",
                    transactionId);
            return true;
        }

        try {
            eventPublisher.publishEvent(new TransactionBusinessEvent(transactionId, payload));
            log.info("Local business event dispatched successfully for transaction: {}", transactionId);
            return true;
        } catch (Exception e) {
            log.error("Local business execution failed for transaction: {}", transactionId, e);
            return false;
        }
    }

    /**
     * 发送事务消息
     *
     * @param topic   消息主题
     * @param payload 消息体
     * @return 发送结果
     */
    public boolean sendTransactionMessage(String topic, Object payload) {
        try {
            org.apache.rocketmq.common.message.Message message =
                    new org.apache.rocketmq.common.message.Message(
                            topic,
                            payload.toString().getBytes()
                    );

            // 使用事务发送
            org.apache.rocketmq.client.producer.TransactionSendResult sendResult =
                    rocketMQTemplate.getProducer().sendMessageInTransaction(message, null);

            if (sendResult.getLocalTransactionState() == org.apache.rocketmq.client.producer.LocalTransactionState.COMMIT_MESSAGE) {
                log.info("Transaction message sent successfully: {}", sendResult.getMsgId());
                return true;
            } else {
                log.warn("Transaction message failed: {}, state: {}",
                        sendResult.getMsgId(), sendResult.getLocalTransactionState());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send transaction message", e);
            return false;
        }
    }

    /**
     * 获取事务记录
     *
     * @param transactionId 事务ID
     * @return 事务记录
     */
    public TransactionRecord getTransactionRecord(String transactionId) {
        return transactionRecords.get(transactionId);
    }

    /**
     * 清理已完成的事务记录
     */
    public void cleanupCompletedTransactions() {
        long currentTime = System.currentTimeMillis();
        transactionRecords.entrySet().removeIf(entry -> {
            TransactionRecord record = entry.getValue();
            // 清理已完成且超过24小时的记录
            return (record.getStatus() == TransactionStatus.COMMIT ||
                    record.getStatus() == TransactionStatus.ROLLBACK) &&
                    (currentTime - record.getUpdateTime() > 24 * 60 * 60 * 1000);
        });
        log.info("Cleaned up completed transaction records, remaining: {}", transactionRecords.size());
    }
}
