package com.aisaas.common.mq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版事务状态存储：仅作为 Redis 不可用时的兜底（单实例开发环境）。
 *
 * <p>注意：不具备跨实例/重启一致性，生产环境必须使用
 * {@link RedisTransactionStateStore}。</p>
 */
public class InMemoryTransactionStateStore implements TransactionStateStore {

    private final Map<String, TransactionState> states = new ConcurrentHashMap<>();

    @Override
    public void save(TransactionState state) {
        if (state != null && state.getTransactionId() != null) {
            states.put(state.getTransactionId(), state);
        }
    }

    @Override
    public TransactionState find(String transactionId) {
        return transactionId == null ? null : states.get(transactionId);
    }
}
