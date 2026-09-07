package com.aisaas.common.config;

import com.aisaas.common.mq.InMemoryTransactionStateStore;
import com.aisaas.common.mq.TransactionState;
import com.aisaas.common.mq.TransactionStateStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 事务消息配置
 * 实现事务消息支持配置
 *
 * <p>本地事务状态通过 {@link TransactionStateStore} 持久化（生产为 Redis 实现，
 * Redis 不可用时降级内存实现）：保证服务重启 / 多实例部署后，broker 对未决半消息的
 * 回查仍能依据历史状态正确应答，避免"本地事务已成功、消息却被超时回滚"的不一致。</p>
 */
@Slf4j
@Configuration
public class RocketMQTransactionConfig {

    /** 单条事务回查次数上限：超过视为业务执行异常，建议回滚 */
    private static final int MAX_CHECK_COUNT = 10;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 应用事件发布器：用于在本地事务阶段把业务负载交给业务侧处理
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectProvider<TransactionStateStore> stateStoreProvider;

    /** 事务状态存储：Redis 实现；无 Redis 环境（单测/极简部署）降级内存实现 */
    private TransactionStateStore stateStore;

    @PostConstruct
    public void initStateStore() {
        this.stateStore = stateStoreProvider.getIfAvailable(InMemoryTransactionStateStore::new);
        log.info("Transaction state store resolved: {}", stateStore.getClass().getSimpleName());
    }

    // 事务状态枚举
    public enum TransactionStatus {
        UNKNOWN,    // 未知状态
        COMMIT,     // 提交
        ROLLBACK    // 回滚
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

                // 先落"未决"状态再执行业务：即使执行过程中宕机, 重启后 broker 回查
                // 也能从持久化状态得知该事务曾进入本地执行阶段(UNKNOWN -> 持续回查)
                TransactionState state = new TransactionState(
                        transactionId, TransactionStatus.UNKNOWN.name(),
                        System.currentTimeMillis(), System.currentTimeMillis(), 0, payload);
                stateStore.save(state);

                try {
                    // 执行本地业务逻辑
                    boolean success = executeLocalBusiness(transactionId, payload);

                    if (success) {
                        state.setStatus(TransactionStatus.COMMIT.name());
                        stateStore.save(state);
                        log.info("Local transaction committed: {}", transactionId);
                        return LocalTransactionState.COMMIT_MESSAGE;
                    } else {
                        state.setStatus(TransactionStatus.ROLLBACK.name());
                        stateStore.save(state);
                        log.warn("Local transaction rolled back: {}", transactionId);
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }

                } catch (Exception e) {
                    log.error("Local transaction failed: {}", transactionId, e);
                    state.setStatus(TransactionStatus.ROLLBACK.name());
                    stateStore.save(state);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String transactionId = msg.getTransactionId();
                TransactionState state = stateStore.find(transactionId);

                if (state == null) {
                    // 无记录(从未进入本地执行, 或状态已过 48h TTL): 判定回滚, 防止悬挂半消息
                    log.warn("Transaction record not found: {}", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }

                // 回查计数(读-改-写; 单实例原子, 多实例极端并发下允许少量偏差,
                // 仅用于"超过上限强制回滚"的保护, 不影响正确性)
                state.setCheckCount(state.getCheckCount() + 1);
                stateStore.save(state);

                log.info("Checking local transaction: {}, status: {}, check count: {}",
                        transactionId, state.getStatus(), state.getCheckCount());

                if (TransactionStatus.COMMIT.name().equals(state.getStatus())) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                if (TransactionStatus.ROLLBACK.name().equals(state.getStatus())) {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                // UNKNOWN: 如果检查次数过多，可能是业务执行出现问题，建议回滚
                if (state.getCheckCount() > MAX_CHECK_COUNT) {
                    log.warn("Too many checks for transaction: {}, rolling back", transactionId);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.UNKNOW;
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
}
