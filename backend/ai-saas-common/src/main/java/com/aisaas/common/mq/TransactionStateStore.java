package com.aisaas.common.mq;

/**
 * RocketMQ 本地事务状态存储抽象。
 *
 * <p>事务状态必须独立于服务实例生命周期：broker 对未决半消息会持续回查
 * {@code checkLocalTransaction}，若状态只存内存，服务重启/多实例部署后回查
 * 将拿不到结果（只能被动 UNKNOWN → 超时回滚），造成"本地事务已成功、消息却被回滚"
 * 的不一致。持久化到 Redis 后，重启后仍能依据历史状态正确应答回查。</p>
 */
public interface TransactionStateStore {

    /** 保存/更新事务状态（实现方应设置过期时间，避免状态无限堆积） */
    void save(TransactionState state);

    /** 读取事务状态，不存在返回 null */
    TransactionState find(String transactionId);
}
