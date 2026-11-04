package com.aisaas.common.mq;

import com.aisaas.common.util.RedisUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的事务状态存储（生产实现）。
 *
 * <p>关键点：</p>
 * <ul>
 *   <li>状态独立于服务实例：服务重启 / 多实例部署后，broker 回查仍能依据 Redis
 *       中的历史状态正确应答（COMMIT/ROLLBACK），而不是被动 UNKNOWN 等超时回滚；</li>
 *   <li>每次 save 刷新 48h TTL：未决事务足够 broker 回查消化，已完成事务自动过期，
 *       无需额外清理任务；</li>
 *   <li>启动恢复：扫描未决(UNKNOWN)且长时间未更新的事务记录并告警——这类事务的
 *       本地业务结果未知，交由 broker 持续回查决策，运维侧需人工介入确认。</li>
 * </ul>
 */
@Slf4j
@Component
public class RedisTransactionStateStore implements TransactionStateStore {

    static final String KEY_PREFIX = "rocketmq:tx:";
    private static final long TTL_HOURS = 48;
    /** 未决事务超过该时长仍未更新视为"疑似悬挂"，启动时告警 */
    private static final long STALE_THRESHOLD_MS = 10 * 60 * 1000L;

    private final RedisUtils redisUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTransactionStateStore(RedisUtils redisUtils,
                                      RedisTemplate<String, Object> redisTemplate) {
        this.redisUtils = redisUtils;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void recoverUnfinishedTransactions() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("[TxState] 事务状态恢复检查完成: 无历史事务记录");
                return;
            }
            int stale = 0;
            for (String key : keys) {
                Object raw = redisUtils.get(key);
                if (!(raw instanceof TransactionState)) {
                    continue;
                }
                TransactionState state = (TransactionState) raw;
                if ("UNKNOWN".equals(state.getStatus())
                        && System.currentTimeMillis() - state.getUpdateTime() > STALE_THRESHOLD_MS) {
                    stale++;
                    log.warn("[TxState] 发现疑似悬挂事务(本地业务结果未知, 交由 broker 回查决策, 请人工确认): txId={}, updateTime={}, checkCount={}",
                            state.getTransactionId(), state.getUpdateTime(), state.getCheckCount());
                }
            }
            log.info("[TxState] 事务状态恢复检查完成: 历史记录 {} 条, 其中疑似悬挂 {} 条", keys.size(), stale);
        } catch (Exception e) {
            // 恢复检查失败不阻塞启动, broker 回查仍可依据 Redis 状态正常应答
            log.warn("[TxState] 事务状态恢复检查失败(不影响主流程): {}", e.getMessage());
        }
    }

    @Override
    public void save(TransactionState state) {
        if (state == null || state.getTransactionId() == null) {
            return;
        }
        state.setUpdateTime(System.currentTimeMillis());
        redisUtils.set(KEY_PREFIX + state.getTransactionId(), state, TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public TransactionState find(String transactionId) {
        if (transactionId == null) {
            return null;
        }
        Object raw = redisUtils.get(KEY_PREFIX + transactionId);
        return raw instanceof TransactionState ? (TransactionState) raw : null;
    }
}
