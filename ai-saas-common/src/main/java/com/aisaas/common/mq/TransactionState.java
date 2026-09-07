package com.aisaas.common.mq;

import java.io.Serializable;

/**
 * RocketMQ 事务消息的本地事务状态（可持久化 DTO）。
 *
 * <p>无参构造 + getter/setter 是为了兼容 RedisTemplate 的
 * {@code GenericJackson2JsonRedisSerializer}（需写入/读取 @class 类型信息）。</p>
 */
public class TransactionState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事务ID（RocketMQ transactionId） */
    private String transactionId;

    /** 状态：UNKNOWN / COMMIT / ROLLBACK（与 RocketMQTransactionConfig.TransactionStatus 对应） */
    private String status;

    /** 创建时间（毫秒） */
    private long createTime;

    /** 最后更新时间（毫秒） */
    private long updateTime;

    /** broker 回查次数 */
    private int checkCount;

    /** 业务负载（JSON 字符串，便于排查） */
    private String payload;

    public TransactionState() {
    }

    public TransactionState(String transactionId, String status, long createTime,
                            long updateTime, int checkCount, String payload) {
        this.transactionId = transactionId;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.checkCount = checkCount;
        this.payload = payload;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }

    public int getCheckCount() { return checkCount; }
    public void setCheckCount(int checkCount) { this.checkCount = checkCount; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
