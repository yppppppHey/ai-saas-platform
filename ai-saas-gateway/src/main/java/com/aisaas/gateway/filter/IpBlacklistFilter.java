package com.aisaas.gateway.filter;

import com.aisaas.gateway.config.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IP黑白名单过滤器
 * 提供基于IP的访问控制，支持配置和动态管理
 */
@Slf4j
@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {

    private static final String IP_BLACKLIST_REDIS_KEY = "gateway:ip:blacklist";
    private static final String IP_WHITELIST_REDIS_KEY = "gateway:ip:whitelist";
    private static final String IP_BLOCK_PREFIX = "gateway:ip:block:";
    
    // 本地缓存
    private final Set<String> localBlacklist = ConcurrentHashMap.newKeySet();
    private final Set<String> localWhitelist = ConcurrentHashMap.newKeySet();
    private volatile long lastSyncTime = 0;
    private static final long SYNC_INTERVAL_MS = 30000; // 30秒同步一次

    @Autowired
    private GatewayConfig gatewayConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        // 初始化时从配置加载IP名单
        loadIpListsFromConfig();
        // 从Redis同步
        syncFromRedis();
        log.info("IP Blacklist Filter initialized. Blacklist: {}, Whitelist: {}", 
                localBlacklist.size(), localWhitelist.size());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        String path = request.getPath().value();

        // 定期同步Redis中的名单
        syncFromRedisIfNeeded();

        // 1. 检查IP是否被临时封禁（基于Redis）
        if (isIpTempBlocked(clientIp)) {
            log.warn("Request from temporarily blocked IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "IP地址已被临时封禁，请稍后再试");
        }

        // 2. 检查IP白名单（如果启用了白名单模式）
        if (isWhitelistEnabled()) {
            if (!isIpInWhitelist(clientIp)) {
                log.warn("Request from non-whitelisted IP: {} - Path: {}", clientIp, path);
                return forbidden(exchange, "IP地址不在白名单中");
            }
            // 在白名单中，直接放行
            return chain.filter(exchange);
        }

        // 3. 检查IP黑名单
        if (isIpInBlacklist(clientIp)) {
            log.warn("Request from blacklisted IP: {} - Path: {}", clientIp, path);
            return forbidden(exchange, "IP地址已被列入黑名单");
        }

        // 记录访问日志
        if (log.isDebugEnabled()) {
            log.debug("IP check passed for {} - Path: {}", clientIp, path);
        }

        return chain.filter(exchange);
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
     * 如果需要，从Redis同步名单
     */
    private void syncFromRedisIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime > SYNC_INTERVAL_MS) {
            synchronized (this) {
                if (currentTime - lastSyncTime > SYNC_INTERVAL_MS) {
                    syncFromRedis();
                    lastSyncTime = currentTime;
                }
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
     * 检查IP是否被临时封禁
     */
    private boolean isIpTempBlocked(String ip) {
        try {
            String blockKey = IP_BLOCK_PREFIX + ip;
            Boolean isBlocked = redisTemplate.hasKey(blockKey);
            return Boolean.TRUE.equals(isBlocked);
        } catch (Exception e) {
            log.error("Error checking temp block for IP {}: {}", ip, e.getMessage());
            return false;
        }
    }

    /**
     * 是否启用了白名单
     */
    private boolean isWhitelistEnabled() {
        GatewayConfig.SecurityConfig security = gatewayConfig.getSecurity();
        return security != null && security.isIpWhitelistEnabled();
    }

    /**
     * 检查IP是否在白名单中
     */
    private boolean isIpInWhitelist(String ip) {
        return localWhitelist.contains(ip) || isIpInRangeList(ip, localWhitelist);
    }

    /**
     * 检查IP是否在黑名单中
     */
    private boolean isIpInBlacklist(String ip) {
        return localBlacklist.contains(ip) || isIpInRangeList(ip, localBlacklist);
    }

    /**
     * 检查IP是否在CIDR范围列表中
     */
    private boolean isIpInRangeList(String ip, Set<String> rangeList) {
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

            InetAddress networkAddress = InetAddress.getByName(network);
            InetAddress ipAddress = InetAddress.getByName(ip);

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

    /**
     * 返回403禁止访问响应
     */
    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getAttribute(TRACE_ID_KEY);

        String body = String.format(
                "{\"code\":403,\"message\":\"%s\",\"data\":null,\"traceId\":\"%s\",\"timestamp\":%d}",
                message,
                traceId != null ? traceId : "",
                System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 返回413请求实体过大响应
     */
    private Mono<Void> payloadTooLarge(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getAttribute(TRACE_ID_KEY);

        String body = String.format(
                "{\"code\":413,\"message\":\"请求实体过大\",\"data\":null,\"traceId\":\"%s\",\"timestamp\":%d}",
                traceId != null ? traceId : "",
                System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // 在最高优先级之后
    }
}
