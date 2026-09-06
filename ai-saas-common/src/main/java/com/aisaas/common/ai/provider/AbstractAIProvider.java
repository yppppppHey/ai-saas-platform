package com.aisaas.common.ai.provider;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AI Provider 抽象基类
 * 封装通用的HTTP请求处理和响应解析逻辑
 */
@Slf4j
public abstract class AbstractAIProvider implements AIProvider {

    protected final WebClient webClient;
    protected final String apiKey;
    protected final String baseUrl;

    // Token价格配置 (USD per 1K tokens)
    protected final Map<String, ModelPriceConfig> modelPrices = new HashMap<>();
    protected final Map<String, Integer> modelTokenLimits = new HashMap<>();

    protected AbstractAIProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        initModelConfigs();
    }

    /**
     * 初始化模型配置（价格和Token限制）
     */
    protected abstract void initModelConfigs();

    /**
     * 添加模型配置
     */
    protected void addModelConfig(String model, int tokenLimit, double inputPrice, double outputPrice) {
        modelTokenLimits.put(model, tokenLimit);
        modelPrices.put(model, new ModelPriceConfig(inputPrice, outputPrice));
    }

    /**
     * 构建聊天请求体
     */
    protected abstract Map<String, Object> buildChatRequestBody(ChatRequest request);

    /**
     * 解析聊天响应
     */
    protected abstract ChatResponse parseChatResponse(String responseBody);

    /**
     * 解析流式聊天响应
     */
    protected abstract ChatResponse parseStreamChatResponse(String line);

    /**
     * 构建嵌入请求体
     */
    protected abstract Map<String, Object> buildEmbeddingRequestBody(EmbeddingRequest request);

    /**
     * 解析嵌入响应
     */
    protected abstract EmbeddingResponse parseEmbeddingResponse(String responseBody);

    @Override
    public boolean supportsModel(String model) {
        return modelTokenLimits.containsKey(model) ||
               Arrays.stream(getSupportedModels()).anyMatch(m -> m.equals(model));
    }

    @Override
    public int getModelTokenLimit(String model) {
        return modelTokenLimits.getOrDefault(model, 4096);
    }

    @Override
    public int estimateTokenCount(String text) {
        // 简化的token估算：约4个字符1个token
        return text == null ? 0 : text.length() / 4 + 1;
    }

    @Override
    public ModelPrice getModelPrice(String model) {
        ModelPriceConfig config = modelPrices.get(model);
        if (config == null) {
            return new ModelPrice(0.0, 0.0);
        }
        return new ModelPrice(config.inputPrice, config.outputPrice);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            log.debug("Sending chat request to {} with model: {}", getProviderName(), request.getModel());

            Map<String, Object> requestBody = buildChatRequestBody(request);

            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException(
                                            "API Error: " + clientResponse.statusCode() + " - " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();

            ChatResponse response = parseChatResponse(responseBody);
            response.setProvider(getProviderName());

            log.debug("Chat response received: {}", response.getId());
            return response;

        } catch (Exception e) {
            log.error("Chat request failed for provider: {}", getProviderName(), e);
            return ChatResponse.builder()
                    .provider(getProviderName())
                    .error(ChatResponse.ErrorInfo.builder()
                            .code("REQUEST_FAILED")
                            .message(e.getMessage())
                            .type("api_error")
                            .build())
                    .build();
        }
    }

    @Override
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        log.debug("Starting stream chat request to {} with model: {}", getProviderName(), request.getModel());

        Map<String, Object> requestBody = buildChatRequestBody(request);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException(
                                        "API Error: " + clientResponse.statusCode() + " - " + errorBody)))
                )
                .bodyToFlux(String.class)
                .filter(line -> !line.trim().isEmpty())
                .map(line -> {
                    try {
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6);
                            if ("[DONE]".equals(jsonData.trim())) {
                                return null;
                            }
                            ChatResponse response = parseStreamChatResponse(jsonData);
                            response.setProvider(getProviderName());
                            return response;
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Failed to parse stream response: {}", line, e);
                        return null;
                    }
                })
                .filter(response -> response != null);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        if (request.isBatch()) {
            return embedBatch(request);
        }
        return doEmbed(request);
    }

    @Override
    public EmbeddingResponse embedBatch(EmbeddingRequest request) {
        if (!request.isBatch()) {
            // 如果批量列表为空，使用单条input
            if (request.getInput() != null) {
                return doEmbed(request);
            }
            return EmbeddingResponse.builder()
                    .error(EmbeddingResponse.ErrorInfo.builder()
                            .code("INVALID_REQUEST")
                            .message("Batch request must have inputs list")
                            .build())
                    .build();
        }
        return doEmbed(request);
    }

    private EmbeddingResponse doEmbed(EmbeddingRequest request) {
        try {
            log.debug("Sending embedding request to {} with model: {}", getProviderName(), request.getModel());

            Map<String, Object> requestBody = buildEmbeddingRequestBody(request);

            String responseBody = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException(
                                            "API Error: " + clientResponse.statusCode() + " - " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();

            EmbeddingResponse response = parseEmbeddingResponse(responseBody);
            response.setProvider(getProviderName());

            log.debug("Embedding response received: {}", response.getData().size());
            return response;

        } catch (Exception e) {
            log.error("Embedding request failed for provider: {}", getProviderName(), e);
            return EmbeddingResponse.builder()
                    .provider(getProviderName())
                    .error(EmbeddingResponse.ErrorInfo.builder()
                            .code("REQUEST_FAILED")
                            .message(e.getMessage())
                            .build())
                    .build();
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            // 发送一个简单的embedding请求来检查健康状态
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(getSupportedModels()[0])
                    .input("test")
                    .build();

            EmbeddingResponse response = embed(request);
            return response.isSuccess();
        } catch (Exception e) {
            log.warn("Health check failed for provider: {}", getProviderName(), e);
            return false;
        }
    }

    /**
     * 模型价格配置
     */
    protected static class ModelPriceConfig {
        final double inputPrice;
        final double outputPrice;

        ModelPriceConfig(double inputPrice, double outputPrice) {
            this.inputPrice = inputPrice;
            this.outputPrice = outputPrice;
        }
    }
}
