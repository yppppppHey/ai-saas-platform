package com.aisaas.common.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Nacos 配置版本管理器
 * 实现配置版本管理、动态配置刷新和配置历史记录
 */
@Slf4j
@Component
public class NacosConfigVersionManager {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    // 配置历史记录存储
    private final Map<String, ConfigHistory> configHistoryMap = new ConcurrentHashMap<>();

    // 配置监听器Map
    private final Map<String, NacosConfigListener> listenerMap = new ConcurrentHashMap<>();

    // 版本号生成器
    private final AtomicLong versionGenerator = new AtomicLong(1);

    // 线程池用于配置刷新
    private final Executor refreshExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nacos-config-refresh-" + versionGenerator.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        log.info("Initializing NacosConfigVersionManager for application: {} with profile: {}",
                applicationName, activeProfile);
    }

    @PreDestroy
    public void destroy() {
        log.info("Destroying NacosConfigVersionManager...");
        // 移除所有监听器
        listenerMap.forEach((dataId, listener) -> {
            try {
                configService.removeListener(dataId, getGroup(), listener);
            } catch (Exception e) {
                log.error("Failed to remove listener for: {}", dataId, e);
            }
        });
        listenerMap.clear();
    }

    /**
     * 获取配置
     *
     * @param dataId 配置ID
     * @return 配置内容
     */
    public String getConfig(String dataId) {
        try {
            return configService.getConfig(dataId, getGroup(), 5000);
        } catch (NacosException e) {
            log.error("Failed to get config: {}", dataId, e);
            return null;
        }
    }

    /**
     * 发布配置
     *
     * @param dataId    配置ID
     * @param content   配置内容
     * @return 是否成功
     */
    public boolean publishConfig(String dataId, String content) {
        try {
            // 生成新版本号
            long version = versionGenerator.getAndIncrement();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

            // 添加版本信息
            JsonNode jsonNode = objectMapper.readTree(content);
            if (jsonNode instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode.put("_version", version);
                objectNode.put("_timestamp", timestamp);
                content = objectNode.toString();
            }

            // 发布配置
            boolean success = configService.publishConfig(dataId, getGroup(), content);

            if (success) {
                // 保存历史记录
                saveConfigHistory(dataId, content, version, timestamp);
                log.info("Published config: {} with version: {}", dataId, version);
            }

            return success;
        } catch (Exception e) {
            log.error("Failed to publish config: {}", dataId, e);
            return false;
        }
    }

    /**
     * 删除配置
     *
     * @param dataId 配置ID
     * @return 是否成功
     */
    public boolean removeConfig(String dataId) {
        try {
            boolean success = configService.removeConfig(dataId, getGroup());
            if (success) {
                configHistoryMap.remove(dataId);
                listenerMap.remove(dataId);
                log.info("Removed config: {}", dataId);
            }
            return success;
        } catch (NacosException e) {
            log.error("Failed to remove config: {}", dataId, e);
            return false;
        }
    }

    /**
     * 添加配置监听器
     *
     * @param dataId   配置ID
     * @param listener 配置变更监听器
     */
    public void addListener(String dataId, ConfigChangeListener listener) {
        try {
            NacosConfigListener nacosListener = new NacosConfigListener(dataId, listener);
            configService.addListener(dataId, getGroup(), nacosListener);
            listenerMap.put(dataId, nacosListener);
            log.info("Added config listener for: {}", dataId);
        } catch (Exception e) {
            log.error("Failed to add listener for: {}", dataId, e);
        }
    }

    /**
     * 移除配置监听器
     *
     * @param dataId 配置ID
     */
    public void removeListener(String dataId) {
        NacosConfigListener listener = listenerMap.remove(dataId);
        if (listener != null) {
            configService.removeListener(dataId, getGroup(), listener);
            log.info("Removed config listener for: {}", dataId);
        }
    }

    /**
     * 获取配置历史记录
     *
     * @param dataId 配置ID
     * @return 历史记录
     */
    public ConfigHistory getConfigHistory(String dataId) {
        return configHistoryMap.get(dataId);
    }

    /**
     * 获取所有配置历史
     *
     * @return 所有历史记录
     */
    public Map<String, ConfigHistory> getAllConfigHistory() {
        return new HashMap<>(configHistoryMap);
    }

    // ============ 私有方法 ============

    private String getGroup() {
        return activeProfile;
    }

    private void saveConfigHistory(String dataId, String content, long version, String timestamp) {
        ConfigHistory history = configHistoryMap.computeIfAbsent(dataId, k -> new ConfigHistory(dataId));
        history.addVersion(content, version, timestamp);
        configHistoryMap.put(dataId, history);
    }

    // ============ 内部类 ============

    /**
     * Nacos配置监听器
     */
    private class NacosConfigListener implements com.alibaba.nacos.api.config.listener.Listener {
        private final String dataId;
        private final ConfigChangeListener listener;

        public NacosConfigListener(String dataId, ConfigChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        @Override
        public void receiveConfigInfo(String config) {
            log.info("Config changed: {}", dataId);
            refreshExecutor.execute(() -> {
                try {
                    listener.onConfigChange(dataId, config);
                } catch (Exception e) {
                    log.error("Failed to handle config change: {}", dataId, e);
                }
            });
        }

        @Override
        public Executor getExecutor() {
            return null; // 使用默认线程池
        }
    }

    // ============ 数据类 ============

    /**
     * 配置变更监听器接口
     */
    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigChange(String dataId, String config);
    }

    /**
     * 配置历史记录
     */
    @Data
    public static class ConfigHistory {
        private final String dataId;
        private final java.util.List<ConfigVersion> versions;
        private long currentVersion;

        public ConfigHistory(String dataId) {
            this.dataId = dataId;
            this.versions = new java.util.ArrayList<>();
            this.currentVersion = 0;
        }

        public void addVersion(String content, long version, String timestamp) {
            versions.add(new ConfigVersion(content, version, timestamp));
            this.currentVersion = version;
        }

        public ConfigVersion getLatestVersion() {
            return versions.isEmpty() ? null : versions.get(versions.size() - 1);
        }
    }

    /**
     * 配置版本
     */
    @Data
    @AllArgsConstructor
    public static class ConfigVersion {
        private String content;
        private long version;
        private String timestamp;
    }
}
