package com.aisaas.common.session;

import com.aisaas.common.util.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 会话管理器
 * 实现用户会话存储方案和 Token 黑名单机制
 */
@Slf4j
@Component
public class SessionManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 会话默认过期时间（7天）
    private static final long DEFAULT_SESSION_TIMEOUT = 7 * 24 * 60 * 60;
    // Token黑名单过期时间（与JWT过期时间一致）
    private static final long DEFAULT_BLACKLIST_TIMEOUT = 24 * 60 * 60;

    /**
     * 创建用户会话
     *
     * @param token    用户token
     * @param userId   用户ID
     * @param metadata 会话元数据
     */
    public void createSession(String token, Long userId, Map<String, Object> metadata) {
        String sessionKey = RedisKeys.userSession(token);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("token", token);
        sessionData.put("createTime", System.currentTimeMillis());

        if (metadata != null) {
            sessionData.putAll(metadata);
        }

        redisTemplate.opsForHash().putAll(sessionKey, sessionData);
        redisTemplate.expire(sessionKey, DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);

        // 维护用户到会话的映射（支持单点登录）
        String userSessionsKey = "ai:platform:user:sessions:" + userId;
        redisTemplate.opsForSet().add(userSessionsKey, token);
        redisTemplate.expire(userSessionsKey, DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);

        log.info("Created session for user: {} with token: {}", userId, token);
    }

    /**
     * 获取会话信息
     *
     * @param token 用户token
     * @return 会话数据
     */
    public Map<String, Object> getSession(String token) {
        String sessionKey = RedisKeys.userSession(token);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(sessionKey);

        if (entries.isEmpty()) {
            return null;
        }

        Map<String, Object> sessionData = new HashMap<>();
        entries.forEach((k, v) -> sessionData.put(k.toString(), v));
        return sessionData;
    }

    /**
     * 更新会话信息
     *
     * @param token    用户token
     * @param metadata 要更新的元数据
     */
    public void updateSession(String token, Map<String, Object> metadata) {
        String sessionKey = RedisKeys.userSession(token);

        if (metadata != null && !metadata.isEmpty()) {
            redisTemplate.opsForHash().putAll(sessionKey, metadata);
        }

        log.debug("Updated session for token: {}", token);
    }

    /**
     * 刷新会话过期时间
     *
     * @param token 用户token
     */
    public void refreshSession(String token) {
        String sessionKey = RedisKeys.userSession(token);
        redisTemplate.expire(sessionKey, DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);

        Map<String, Object> session = getSession(token);
        if (session != null) {
            Object userId = session.get("userId");
            if (userId != null) {
                String userSessionsKey = "ai:platform:user:sessions:" + userId;
                redisTemplate.expire(userSessionsKey, DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);
            }
        }

        log.debug("Refreshed session for token: {}", token);
    }

    /**
     * 销毁会话
     *
     * @param token 用户token
     */
    public void destroySession(String token) {
        String sessionKey = RedisKeys.userSession(token);

        // 获取用户ID以便从用户会话集合中移除
        Map<String, Object> session = getSession(token);
        if (session != null) {
            Object userId = session.get("userId");
            if (userId != null) {
                String userSessionsKey = "ai:platform:user:sessions:" + userId;
                redisTemplate.opsForSet().remove(userSessionsKey, token);
            }
        }

        redisTemplate.delete(sessionKey);
        log.info("Destroyed session for token: {}", token);
    }

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话token集合
     */
    public Set<Object> getUserSessions(Long userId) {
        String userSessionsKey = "ai:platform:user:sessions:" + userId;
        return redisTemplate.opsForSet().members(userSessionsKey);
    }

    /**
     * 销毁用户的所有会话（单点登出）
     *
     * @param userId 用户ID
     */
    public void destroyAllUserSessions(Long userId) {
        String userSessionsKey = "ai:platform:user:sessions:" + userId;
        Set<Object> sessions = redisTemplate.opsForSet().members(userSessionsKey);

        if (sessions != null) {
            for (Object token : sessions) {
                String sessionKey = RedisKeys.userSession(token.toString());
                redisTemplate.delete(sessionKey);
            }
        }

        redisTemplate.delete(userSessionsKey);
        log.info("Destroyed all sessions for user: {}", userId);
    }

    // ========== Token 黑名单机制 ==========

    /**
     * 将 Token 加入黑名单
     *
     * @param token     要加入黑名单的token
     * @param expireIn  过期时间（秒）
     */
    public void addToBlacklist(String token, long expireIn) {
        String blacklistKey = RedisKeys.refreshToken(token); // 复用refreshToken的key格式
        redisTemplate.opsForValue().set(blacklistKey, "1", expireIn, TimeUnit.SECONDS);
        log.info("Added token to blacklist: {}", token);
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token 要检查的token
     * @return true表示在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String blacklistKey = RedisKeys.refreshToken(token);
        Boolean exists = redisTemplate.hasKey(blacklistKey);
        return exists != null && exists;
    }

    /**
     * 从黑名单移除 Token
     *
     * @param token 要移除的token
     */
    public void removeFromBlacklist(String token) {
        String blacklistKey = RedisKeys.refreshToken(token);
        redisTemplate.delete(blacklistKey);
        log.info("Removed token from blacklist: {}", token);
    }
}
