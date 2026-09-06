package com.aisaas.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 热点数据缓存管理器
 * 实现热点数据缓存策略和缓存失效与刷新机制
 */
@Slf4j
@Component
public class HotDataCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 本地缓存（二级缓存）
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    // 热点数据Key前缀
    private static final String HOT_DATA_PREFIX = "ai:platform:hotdata:";

    // 默认热点数据过期时间（10分钟）
    private static final long DEFAULT_HOT_DATA_TTL = 10 * 60;

    // 本地缓存过期时间（1分钟）
    private static final long LOCAL_CACHE_TTL = 60 * 1000;

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long expireTime;

        public CacheEntry(Object value, long ttlMillis) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public Object getValue() {
            return value;
        }
    }

    /**
     * 设置热点数据缓存
     *
     * @param key       缓存Key
     * @param value     缓存值
     * @param ttl       过期时间（秒）
     * @param useLocal  是否使用本地缓存
     */
    public void set(String key, Object value, long ttl, boolean useLocal) {
        String cacheKey = HOT_DATA_PREFIX + key;

        try {
            // 写入Redis
            redisTemplate.opsForValue().set(cacheKey, value, ttl, TimeUnit.SECONDS);

            // 写入本地缓存
            if (useLocal) {
                localCache.put(cacheKey, new CacheEntry(value, LOCAL_CACHE_TTL));
            }

            log.debug("Set hot data cache: {}, ttl: {}s", key, ttl);
        } catch (Exception e) {
            log.error("Failed to set hot data cache: {}", key, e);
        }
    }

    /**
     * 设置热点数据缓存（使用默认过期时间）
     *
     * @param key   缓存Key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_HOT_DATA_TTL, true);
    }

    /**
     * 获取热点数据缓存
     *
     * @param key  缓存Key
     * @param type 返回值类型
     * @param <T>  泛型类型
     * @return 缓存值，不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        String cacheKey = HOT_DATA_PREFIX + key;

        try {
            // 1. 先查本地缓存
            CacheEntry localEntry = localCache.get(cacheKey);
            if (localEntry != null) {
                if (!localEntry.isExpired()) {
                    log.debug("Hit local cache: {}", key);
                    return (T) localEntry.getValue();
                } else {
                    // 本地缓存过期，移除
                    localCache.remove(cacheKey);
                }
            }

            // 2. 查询Redis
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                // 刷新本地缓存
                localCache.put(cacheKey, new CacheEntry(value, LOCAL_CACHE_TTL));
                log.debug("Hit Redis cache: {}", key);
                return (T) value;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to get hot data cache: {}", key, e);
            return null;
        }
    }

    /**
     * 删除热点数据缓存
     *
     * @param key 缓存Key
     */
    public void delete(String key) {
        String cacheKey = HOT_DATA_PREFIX + key;

        try {
            // 删除Redis缓存
            redisTemplate.delete(cacheKey);

            // 删除本地缓存
            localCache.remove(cacheKey);

            log.debug("Deleted hot data cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete hot data cache: {}", key, e);
        }
    }

    /**
     * 批量删除热点数据缓存
     *
     * @param keys 缓存Key集合
     */
    public void deleteBatch(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            for (String key : keys) {
                delete(key);
            }
            log.debug("Deleted {} hot data cache entries", keys.size());
        } catch (Exception e) {
            log.error("Failed to batch delete hot data cache", e);
        }
    }

    /**
     * 刷新缓存（删除并重新加载）
     *
     * @param key      缓存Key
     * @param loader   数据加载器
     * @param ttl      过期时间（秒）
     * @param <T>      泛型类型
     * @return 刷新后的数据
     */
    public <T> T refresh(String key, DataLoader<T> loader, long ttl) {
        // 删除旧缓存
        delete(key);

        // 加载新数据
        T data = loader.load();
        if (data != null) {
            set(key, data, ttl, true);
        }

        return data;
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.clear();
        log.info("Cleared local cache");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("localCacheSize", localCache.size());
        stats.put("localCacheExpiredCount",
                localCache.values().stream().filter(CacheEntry::isExpired).count());
        return stats;
    }

    /**
     * 数据加载器接口
     *
     * @param <T> 数据类型
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load();
    }
}
