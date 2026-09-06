package com.aisaas.chat.service;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型智能路由与故障降级
 *
 * 降级链模型（可配置）：
 *   ai.failover.chains.gpt-4 = gpt-3.5-turbo,deepseek-chat
 *   ai.failover.default = deepseek-chat
 *
 * 语义：
 * - 主模型调用异常（网络/超时/限流）或返回失败时，沿降级链依次重试
 * - 每次切换都记录 failover 事件日志（降级率可从日志统计）
 * - 降级后实际使用的模型以 ChatResponse.model 为准，计费链路据此按真实模型计价
 * - 链内跨 Provider：按模型ID自动路由到支持该模型的 Provider（AIProviderFactory.getProviderByModel）
 *
 * 设计取舍：计费/用量消息是异步旁路，这里不引入熔断器状态机，
 * 保持"请求级重试"简单语义；后续可在该层加滑动窗口熔断。
 */
@Slf4j
@Service
public class ModelFailoverService {

    private final AIProviderFactory providerFactory;
    private final Map<String, List<String>> chains = new HashMap<>();
    private final List<String> defaultChain;

    public ModelFailoverService(AIProviderFactory providerFactory,
                                @Value("${ai.failover.chains:}") Map<String, String> chainConfig,
                                @Value("${ai.failover.default:deepseek-chat}") String defaultFallback) {
        this.providerFactory = providerFactory;
        if (chainConfig != null) {
            chainConfig.forEach((model, fallbacks) -> {
                if (StringUtils.hasText(fallbacks)) {
                    chains.put(model, split(fallbacks));
                }
            });
        }
        this.defaultChain = split(defaultFallback);
        log.info("模型降级链配置完成: {} 组, 默认降级: {}", chains.size(), defaultChain);
    }

    private List<String> split(String csv) {
        List<String> result = new ArrayList<>();
        for (String s : csv.split(",")) {
            if (StringUtils.hasText(s)) {
                result.add(s.trim());
            }
        }
        return result;
    }

    /**
     * 构建降级链：主模型 + 备选模型（去重、跳过主模型自身）
     */
    List<String> buildChain(String primaryModel) {
        List<String> chain = new ArrayList<>();
        chain.add(primaryModel);
        List<String> fallbacks = chains.getOrDefault(primaryModel, defaultChain);
        for (String f : fallbacks) {
            if (!chain.contains(f)) {
                chain.add(f);
            }
        }
        return chain;
    }

    /**
     * 解析某个模型应由哪个 Provider 服务（优先显式 providerName，其次按模型路由）
     */
    AIProvider resolveProvider(String providerName, String modelId) {
        if (StringUtils.hasText(providerName)) {
            AIProvider explicit = providerFactory.getProvider(providerName);
            if (explicit != null && explicit.supportsModel(modelId)) {
                return explicit;
            }
        }
        return providerFactory.getProviderByModel(modelId);
    }

    /**
     * 带降级的同步对话调用
     *
     * @param providerName 首选 Provider（可为空，自动按模型路由）
     * @param request      对话请求（主模型写入 request.model）
     * @return 成功的响应（实际模型以 response.getModel() 为准）
     * @throws com.aisaas.common.exception.BizException 降级链全部失败时抛出
     */
    public ChatResponse chatWithFailover(String providerName, ChatRequest request) {
        List<String> chain = buildChain(request.getModel());
        Exception lastError = null;

        for (int i = 0; i < chain.size(); i++) {
            String modelId = chain.get(i);
            AIProvider provider = resolveProvider(providerName, modelId);
            if (provider == null) {
                log.warn("[failover] 无可用Provider服务模型: {}", modelId);
                continue;
            }

            // 第 0 跳复用原始请求；降级跳换模型
            ChatRequest attempt = request;
            if (i > 0 && !modelId.equals(request.getModel())) {
                attempt = copyWithModel(request, modelId);
            }

            long start = System.currentTimeMillis();
            try {
                ChatResponse response = provider.chat(attempt);
                long cost = System.currentTimeMillis() - start;

                if (response != null && response.isSuccess()) {
                    if (i > 0) {
                        log.warn("[failover] 主模型不可用已降级: {} -> {}, provider={}, 耗时={}ms",
                                chain.get(0), modelId, provider.getProviderName(), cost);
                    }
                    return response;
                }
                String errMsg = response != null && response.getError() != null
                        ? response.getError().getMessage() : "empty response";
                lastError = new IllegalStateException("模型返回失败: " + errMsg);
                log.warn("[failover] 模型返回失败: model={}, provider={}, 耗时={}ms, 原因={}",
                        modelId, provider.getProviderName(), cost, errMsg);
            } catch (Exception e) {
                lastError = e;
                log.warn("[failover] 模型调用异常: model={}, provider={}, 原因={}",
                        modelId, provider.getProviderName(), e.getMessage());
            }
        }

        throw new com.aisaas.common.exception.BizException(
                "所有模型均调用失败(降级链: " + chain + "): "
                        + (lastError != null ? lastError.getMessage() : "未知错误"));
    }

    /**
     * 浅拷贝请求并替换模型（本地构建的请求，直接换 model 即可）
     */
    private ChatRequest copyWithModel(ChatRequest source, String modelId) {
        source.setModel(modelId);
        return source;
    }
}
