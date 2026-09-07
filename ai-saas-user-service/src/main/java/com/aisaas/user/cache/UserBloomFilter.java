package com.aisaas.user.cache;

import com.aisaas.common.util.RedisUtils;
import com.aisaas.user.mapper.UserAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 用户存在性布隆过滤器：防缓存穿透的"预筛"层。
 *
 * 设计要点：
 * 1. 启动时从 DB 全量载入 userId 构建过滤器（expectedInsertions 预留 100w，误判率 1%）；
 * 2. 用户注册时增量 add，避免新建用户被误判为"不存在"；
 * 3. getById 在回源 DB 前先查布隆：判定"一定不存在"才直接返回 null，
 *    从而拦截对不存在 key 的穿透请求，避免打爆 DB；
 * 4. 过滤器不可用时 fail-open（放行走 DB），不挡正常请求。
 */
@Slf4j
@Component
public class UserBloomFilter {

    private final RedisUtils redisUtils;
    private final UserAccountMapper userAccountMapper;

    private static final String FILTER_NAME = "user:bf";
    private static final long EXPECTED_INSERTIONS = 1_000_000L;
    private static final double FALSE_PROBABILITY = 0.01;

    public UserBloomFilter(RedisUtils redisUtils, UserAccountMapper userAccountMapper) {
        this.redisUtils = redisUtils;
        this.userAccountMapper = userAccountMapper;
    }

    @PostConstruct
    public void init() {
        try {
            redisUtils.getBloomFilter(FILTER_NAME, EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            // 全量载入已有 userId（selectObjs 返回首列 = id）
            java.util.List<Object> ids = userAccountMapper.selectObjs(null);
            for (Object id : ids) {
                if (id instanceof Number) {
                    redisUtils.addToBloomFilter(FILTER_NAME, ((Number) id).longValue());
                }
            }
            log.info("[Bloom] 用户存在性布隆过滤器初始化完成, 载入 {} 个 userId", ids.size());
        } catch (Exception e) {
            // 初始化失败不影响主流程, 仅丧失穿透防护
            log.warn("[Bloom] 用户布隆过滤器初始化失败(不影响主流程): {}", e.getMessage());
        }
    }

    /**
     * 该 userId 是否"可能"存在。
     * @return false 表示一定不存在(可安全返回 null, 防穿透); true 表示可能存在(需回源确认)
     */
    public boolean mightContain(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            return redisUtils.containsInBloomFilter(FILTER_NAME, userId);
        } catch (Exception e) {
            // 过滤器不可用时放行, 不挡正常请求
            return true;
        }
    }

    /**
     * 注册新用户时增量加入, 避免被误判为不存在。
     */
    public void add(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisUtils.addToBloomFilter(FILTER_NAME, userId);
        } catch (Exception e) {
            log.warn("[Bloom] 用户布隆过滤器 add 失败: {}", e.getMessage());
        }
    }
}
