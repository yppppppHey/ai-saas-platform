package com.aisaas.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Token 黑名单键工具（网关与 user 服务共用同一套规则）
 *
 * 历史 bug：user 登出写 `ai:platform:token:blacklist:<原文token>`，
 * 网关校验读 `token:blacklist:<SHA256(token)>`——前缀与哈希规则都不一致，
 * 导致"登出后 token 依然可用"。本类把规则收敛到一处，双方必须复用。
 *
 * 为什么存哈希而不是原文：token 是长期凭证，明文落 Redis 会扩大泄露面。
 */
public final class TokenBlacklistSupport {

    private TokenBlacklistSupport() {
    }

    /**
     * 计算 token 的 SHA-256 十六进制摘要
     */
    public static String hashToken(String token) {
        if (token == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 摘要不可用时退化：仍保证同一 token 得到同一 key
            return String.valueOf(token.hashCode());
        }
    }

    /**
     * 黑名单 Redis key（网关与 user 服务必须都用它）
     */
    public static String blacklistKey(String token) {
        return RedisKeys.TOKEN_BLACKLIST + hashToken(token);
    }
}
