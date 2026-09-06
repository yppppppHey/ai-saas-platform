package com.aisaas.common.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Provider 工厂
 * 用于管理和获取不同的AI Provider实例
 */
@Slf4j
@Component
public class AIProviderFactory {

    private final Map<String, AIProvider> providers = new HashMap<>();

    @Autowired
    public AIProviderFactory(List<AIProvider> providerList) {
        for (AIProvider provider : providerList) {
            providers.put(provider.getProviderName().toLowerCase(), provider);
            log.info("Registered AI Provider: {}", provider.getProviderName());
        }
    }

    @PostConstruct
    public void init() {
        log.info("AI Provider Factory initialized with {} providers", providers.size());
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
     * 获取默认的Provider（OpenAI）
     *
     * @return AIProvider实例
     */
    public AIProvider getDefaultProvider() {
        AIProvider openai = providers.get("openai");
        if (openai != null) {
            return openai;
        }
        // 返回第一个可用的Provider
        return providers.values().stream().findFirst().orElse(null);
    }

    /**
     * 获取所有可用的Provider名称
     *
     * @return Provider名称列表
     */
    public java.util.List<String> getAvailableProviders() {
        return providers.keySet().stream().toList();
    }

    /**
     * 检查指定的Provider是否可用
     *
     * @param providerName Provider名称
     * @return 是否可用
     */
    public boolean isProviderAvailable(String providerName) {
        if (providerName == null) {
            return false;
        }
        return providers.containsKey(providerName.toLowerCase());
    }
}