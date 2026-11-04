package com.aisaas.common.util;

/**
 * Redis Key 管理器
 */
public final class RedisKeys {

    private static final String PREFIX = "ai:platform:";

    private RedisKeys() {
    }

    // ========== 用户会话 ==========
    public static String userSession(String token) {
        return PREFIX + "user:session:" + token;
    }

    // ========== 用户配额 ==========
    public static String userQuota(Long userId, String quotaType) {
        return PREFIX + "user:quota:" + userId + ":" + quotaType;
    }

    // ========== 对话上下文 ==========
    public static String chatContext(Long conversationId) {
        return PREFIX + "chat:context:" + conversationId;
    }

    // ========== 流式消息 ==========
    public static String chatStream(Long messageId) {
        return PREFIX + "chat:stream:" + messageId;
    }

    // ========== 限流计数 ==========
    public static String rateLimit(Long userId, String api) {
        return PREFIX + "ratelimit:" + userId + ":" + api;
    }

    // ========== 分布式锁 ==========
    public static String lock(String resource) {
        return PREFIX + "lock:" + resource;
    }

    // ========== 任务进度 ==========
    public static String taskProgress(String taskId) {
        return PREFIX + "task:progress:" + taskId;
    }

    // ========== 幂等令牌 ==========
    public static String idempotent(String token) {
        return PREFIX + "idempotent:" + token;
    }

    // ========== 验证码 ==========
    public static String captcha(String key) {
        return PREFIX + "captcha:" + key;
    }

    // ========== 登录失败计数 ==========
    public static String loginFailCount(String account) {
        return PREFIX + "login:fail:" + account;
    }

    // ========== 刷新令牌 ==========
    public static String refreshToken(String userId) {
        return PREFIX + "refresh:token:" + userId;
    }

    // ========== Token黑名单 ==========
    public static final String TOKEN_BLACKLIST = PREFIX + "token:blacklist:";

    // ========== 用户Token ==========
    public static final String USER_TOKEN = PREFIX + "user:token:";

    // ========== 用户验证码 ==========
    public static final String USER_VERIFY_CODE = PREFIX + "user:verify:code:";

    // ========== 知识库构建状态 ==========
    public static String kbBuildStatus(Long kbId) {
        return PREFIX + "kb:build:" + kbId;
    }

    // ========== 文档处理状态 ==========
    public static String docProcessStatus(String docId) {
        return PREFIX + "doc:process:" + docId;
    }
}
