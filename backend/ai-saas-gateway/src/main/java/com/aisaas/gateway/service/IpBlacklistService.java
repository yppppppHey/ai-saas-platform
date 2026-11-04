package com.aisaas.gateway.service;

import com.aisaas.gateway.config.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IP黑白名单服务
 */
@Slf4j
@Service
public class IpBlacklistService {

    private static final String IP_BLACKLIST_REDIS_KEY = "gateway:ip:blacklist";
    private static final String IP_WHITELIST_REDIS_KEY = "gateway:ip:whitelist";
    private static final String IP_BLOCK_PREFIX = "gateway:ip:block:";

    // 本地缓存
    private final Set<String> localBlacklist = ConcurrentHashMap.newKeySet();
    private final Set<String> localWhitelist = ConcurrentHashMap.newKeySet();

    @Autowired
    private GatewayConfig gatewayConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        // 从配置加载IP名单
        loadIpListsFromConfig();
        // 从Redis同步
        syncFromRedis();
        log.info("IP Blacklist Service initialized. Blacklist: {}, Whitelist: {}",
                localBlacklist.size(), localWhitelist.size());
    }

    /**
     * 从配置加载IP名单
     */
    private void loadIpListsFromConfig() {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        if (security != null) {
            if (!CollectionUtils.isEmpty(security.getIpBlacklist())) {
                localBlacklist.addAll(security.getIpBlacklist());
            }
            if (!CollectionUtils.isEmpty(security.getIpWhitelist())) {
                localWhitelist.addAll(security.getIpWhitelist());
            }
        }
    }

    /**
     * 从Redis同步名单
     */
    private void syncFromRedis() {
        try {
            // 同步黑名单
            Set<String> blacklistFromRedis = redisTemplate.opsForSet().members(IP_BLACKLIST_REDIS_KEY);
            if (!CollectionUtils.isEmpty(blacklistFromRedis)) {
                localBlacklist.addAll(blacklistFromRedis);
            }

            // 同步白名单
            Set<String> whitelistFromRedis = redisTemplate.opsForSet().members(IP_WHITELIST_REDIS_KEY);
            if (!CollectionUtils.isEmpty(whitelistFromRedis)) {
                localWhitelist.addAll(whitelistFromRedis);
            }

            log.debug("Synced IP lists from Redis. Blacklist: {}, Whitelist: {}",
                    localBlacklist.size(), localWhitelist.size());
        } catch (Exception e) {
            log.error("Failed to sync IP lists from Redis: {}", e.getMessage());
        }
    }

    /**
     * 获取黑名单
     */
    public Set<String> getBlacklist() {
        return Set.copyOf(localBlacklist);
    }

    /**
     * 获取白名单
     */
    public Set<String> getWhitelist() {
        return Set.copyOf(localWhitelist);
    }

    /**
     * 添加到黑名单
     */
    public void addToBlacklist(Collection<String> ips) {
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        localBlacklist.addAll(ips);
        redisTemplate.opsForSet().add(IP_BLACKLIST_REDIS_KEY, ips.toArray(new String[0]));
        log.info("Added {} IPs to blacklist", ips.size());
    }

    /**
     * 从黑名单移除
     */
    public void removeFromBlacklist(Collection<String> ips) {
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        localBlacklist.removeAll(ips);
        redisTemplate.opsForSet().remove(IP_BLACKLIST_REDIS_KEY, ips.toArray(new String[0]));
        log.info("Removed {} IPs from blacklist", ips.size());
    }

    /**
     * 添加到白名单
     */
    public void addToWhitelist(Collection<String> ips) {
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        localWhitelist.addAll(ips);
        redisTemplate.opsForSet().add(IP_WHITELIST_REDIS_KEY, ips.toArray(new String[0]));
        log.info("Added {} IPs to whitelist", ips.size());
    }

    /**
     * 从白名单移除
     */
    public void removeFromWhitelist(Collection<String> ips) {
        if (CollectionUtils.isEmpty(ips)) {
            return;
        }
        localWhitelist.removeAll(ips);
        redisTemplate.opsForSet().remove(IP_WHITELIST_REDIS_KEY, ips.toArray(new String[0]));
        log.info("Removed {} IPs from whitelist", ips.size());
    }

    /**
     * 临时封禁IP
     */
    public void blockIp(String ip, int durationMinutes) {
        String blockKey = IP_BLOCK_PREFIX + ip;
        redisTemplate.opsForValue().set(blockKey, String.valueOf(System.currentTimeMillis()),
                durationMinutes, java.util.concurrent.TimeUnit.MINUTES);
        log.info("Blocked IP {} for {} minutes", ip, durationMinutes);
    }

    /**
     * 检查IP是否在黑名单中
     */
    public boolean isInBlacklist(String ip) {
        return localBlacklist.contains(ip) || isIpInRangeList(ip, localBlacklist);
    }

    /**
     * 检查IP是否在白名单中
     */
    public boolean isInWhitelist(String ip) {
        return localWhitelist.contains(ip) || isIpInRangeList(ip, localWhitelist);
    }

    /**
     * 检查IP是否被临时封禁
     */
    public boolean isBlocked(String ip) {
        try {
            String blockKey = IP_BLOCK_PREFIX + ip;
            Boolean isBlocked = redisTemplate.hasKey(blockKey);
            return Boolean.TRUE.equals(isBlocked);
        } catch (Exception e) {
            log.error("Error checking block status for IP {}: {}", ip, e.getMessage());
            return false;
        }
    }

    /**
     * 检查IP是否在CIDR范围列表中
     */
    private boolean isIpInRangeList(String ip, java.util.Set<String> rangeList) {
        for (String range : rangeList) {
            if (range.contains("/")) {
                // CIDR表示法
                if (isIpInCidr(ip, range)) {
                    return true;
                }
            } else if (range.contains("*")) {
                // 通配符
                if (matchWildcard(ip, range)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查IP是否在CIDR范围内
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            java.net.InetAddress networkAddress = java.net.InetAddress.getByName(network);
            java.net.InetAddress ipAddress = java.net.InetAddress.getByName(ip);

            byte[] networkBytes = networkAddress.getAddress();
            byte[] ipBytes = ipAddress.getAddress();

            if (networkBytes.length != ipBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != ipBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (networkBytes[fullBytes] & mask) == (ipBytes[fullBytes] & mask);
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking CIDR for IP {} and CIDR {}: {}", ip, cidr, e.getMessage());
            return false;
        }
    }

    /**
     * 匹配通配符模式
     */
    private boolean matchWildcard(String ip, String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return ip.matches(regex);
    }
}
