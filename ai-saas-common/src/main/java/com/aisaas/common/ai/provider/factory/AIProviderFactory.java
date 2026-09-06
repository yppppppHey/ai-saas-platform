package com.aisaas.common.ai.provider.factory;

import com.aisaas.common.ai.provider.AIProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Provider 工厂类
 * 管理和创建不同的AI Provider实例
 */
@Slf4j
@Component
public class AIProviderFactory {

    /**
     * Provider 实例缓存
     */
    private final Map<String, AIProvider> providers = new ConcurrentHashMap<>();

    /**
     * 自动注入所有 Provider
     */
    @Autowired(required = false)
    private List<AIProvider> providerList = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (providerList != null) {
            for (AIProvider provider : providerList) {
                registerProvider(provider);
            }
        }
        log.info("AIProviderFactory initialized with {} providers", providers.size());
    }

    /**
     * 注册 Provider
     *
     * @param provider Provider实例
     */
    public void registerProvider(AIProvider provider) {
        String providerName = provider.getProviderName().toLowerCase();
        providers.put(providerName, provider);
        log.info("Registered AI Provider: {}", providerName);
    }

    /**
     * 根据名称获取 Provider
     *
     * @param providerName Provider名称
     * @return Provider实例，如果不存在则返回null
     */
    public AIProvider getProvider(String providerName) {
        return providers.get(providerName.toLowerCase());
    }

    /**
     * 根据模型ID获取支持的 Provider
     *
     * @param modelId 模型ID
     * @return 支持该模型的Provider，如果不存在则返回null
     */
    public AIProvider getProviderByModel(String modelId) {
        for (AIProvider provider : providers.values()) {
            if (provider.supportsModel(modelId)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的 Provider
     *
     * @return Provider列表
     */
    public Collection<AIProvider> getAllProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * 获取所有已注册的 Provider 名称
     *
     * @return Provider名称列表
     */
    public List<String> getAllProviderNames() {
        return providers.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取所有支持的模型
     *
     * @return 模型ID到Provider名称的映射
     */
    public Map<String, String> getAllSupportedModels() {
        Map<String, String> models = new HashMap<>();
        for (AIProvider provider : providers.values()) {
            for (String model : provider.getSupportedModels()) {
                models.put(model, provider.getProviderName());
            }
        }
        return models;
    }

    /**
     * 移除 Provider
     *
     * @param providerName Provider名称
     * @return 是否成功移除
     */
    public boolean removeProvider(String providerName) {
        AIProvider removed = providers.remove(providerName.toLowerCase());
        if (removed != null) {
            log.info("Removed AI Provider: {}", providerName);
            return true;
        }
        return false;
    }

    /**
     * 检查 Provider 是否存在
     *
     * @param providerName Provider名称
     * @return 是否存在
     */
    public boolean hasProvider(String providerName) {
        return providers.containsKey(providerName.toLowerCase());
    }
}
