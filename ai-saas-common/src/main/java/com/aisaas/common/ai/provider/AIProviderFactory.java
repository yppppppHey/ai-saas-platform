package com.aisaas.common.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Provider 工厂（合并自原 ai.provider 与 ai.provider.factory 两处重复实现）
 * <p>
 * 管理和获取不同的 AI Provider 实例：
 * - 按名称获取（openai/deepseek）
 * - 按模型ID路由到支持的 Provider（模型智能路由的基础）
 */
@Slf4j
@Component
public class AIProviderFactory {

    private final Map<String, AIProvider> providers = new ConcurrentHashMap<>();

    @Autowired
    public AIProviderFactory(List<AIProvider> providerList) {
        if (providerList != null) {
            for (AIProvider provider : providerList) {
                registerProvider(provider);
            }
        }
    }

    @PostConstruct
    public void init() {
        log.info("AIProviderFactory initialized with {} providers: {}", providers.size(), providers.keySet());
    }

    /**
     * 注册 Provider
     */
    public void registerProvider(AIProvider provider) {
        String providerName = provider.getProviderName().toLowerCase();
        providers.put(providerName, provider);
        log.info("Registered AI Provider: {}", providerName);
    }

    /**
     * 获取指定名称的Provider
     *
     * @param providerName Provider名称 (如 "openai", "deepseek")
     * @return AIProvider实例，如果不存在则返回null
     */
    public AIProvider getProvider(String providerName) {
        if (providerName == null) {
            return null;
        }
        return providers.get(providerName.toLowerCase());
    }

    /**
     * 根据模型ID获取支持的 Provider（模型路由入口）
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
     * 获取默认的Provider（优先 OpenAI）
     */
    public AIProvider getDefaultProvider() {
        AIProvider openai = providers.get("openai");
        if (openai != null) {
            return openai;
        }
        return providers.values().stream().findFirst().orElse(null);
    }

    /**
     * 获取所有已注册的 Provider
     */
    public Collection<AIProvider> getAllProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * 获取所有可用的Provider名称
     */
    public List<String> getAvailableProviders() {
        return providers.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * 获取所有支持的模型（模型ID -> Provider名称）
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
     */
    public boolean removeProvider(String providerName) {
        AIProvider removed = providers.remove(providerName == null ? null : providerName.toLowerCase());
        if (removed != null) {
            log.info("Removed AI Provider: {}", providerName);
            return true;
        }
        return false;
    }

    /**
     * 检查指定的Provider是否可用
     */
    public boolean isProviderAvailable(String providerName) {
        return hasProvider(providerName);
    }

    /**
     * 检查指定的Provider是否已注册
     */
    public boolean hasProvider(String providerName) {
        return providerName != null && providers.containsKey(providerName.toLowerCase());
    }
}
