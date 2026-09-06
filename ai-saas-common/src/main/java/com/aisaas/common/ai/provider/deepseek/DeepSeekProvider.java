package com.aisaas.common.ai.provider.deepseek;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import com.aisaas.common.ai.provider.AbstractAIProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * DeepSeek Provider 实现
 * 支持 DeepSeek-V2、DeepSeek-V2.5、DeepSeek-Coder 等模型
 */
@Slf4j
@Component
public class DeepSeekProvider extends AbstractAIProvider {

    private static final String PROVIDER_NAME = "deepseek";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] SUPPORTED_MODELS = {
            "deepseek-chat",
            "deepseek-reasoner",
            "deepseek-coder",
            "deepseek-v2.5"
    };

    public DeepSeekProvider() {
        super(
                System.getenv().getOrDefault("DEEPSEEK_API_KEY", ""),
                System.getenv().getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
        );
    }

    public DeepSeekProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }

    @Override
    protected void initModelConfigs() {
        // DeepSeek 价格（仅供参考，实际以官方为准）
        // 输入: 1元/百万token, 输出: 2元/百万token
        addModelConfig("deepseek-chat", 64000, 0.001, 0.002);
        addModelConfig("deepseek-reasoner", 64000, 0.001, 0.002);
        addModelConfig("deepseek-coder", 64000, 0.001, 0.002);
        addModelConfig("deepseek-v2.5", 64000, 0.001, 0.002);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String[] getSupportedModels() {
        return SUPPORTED_MODELS;
    }

    @Override
    protected Map<String, Object> buildChatRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();

        // 模型
        body.put("model", request.getModel());

        // 消息
        List<Map<String, Object>> messages = request.getMessages().stream()
                .map(msg -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role", msg.getRole());
                    messageMap.put("content", msg.getContent());
                    if (msg.getName() != null) {
                        messageMap.put("name", msg.getName());
                    }
                    return messageMap;
                })
                .collect(Collectors.toList());
        body.put("messages", messages);

        // 可选参数
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        // 流式输出
        if (Boolean.TRUE.equals(request.getStream())) {
            body.put("stream", true);
        }

        // 额外参数
        if (request.getExtraParams() != null && !request.getExtraParams().isEmpty()) {
            body.putAll(request.getExtraParams());
        }

        return body;
    }

    @Override
    protected ChatResponse parseChatResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ChatResponse.ChatResponseBuilder responseBuilder = ChatResponse.builder()
                    .id(root.path("id").asText())
                    .object(root.path("object").asText())
                    .created(root.path("created").asLong())
                    .model(root.path("model").asText())
                    .provider(PROVIDER_NAME);

            // Parse choices
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isArray()) {
                List<ChatResponse.Choice> choices = new ArrayList<>();
                for (JsonNode choiceNode : choicesNode) {
                    ChatResponse.Choice.ChoiceBuilder choiceBuilder = ChatResponse.Choice.builder()
                            .index(choiceNode.path("index").asInt())
                            .finishReason(choiceNode.path("finish_reason").asText());

                    // Parse message
                    JsonNode messageNode = choiceNode.path("message");
                    if (!messageNode.isMissingNode()) {
                        ChatResponse.Message message = ChatResponse.Message.builder()
                                .role(messageNode.path("role").asText())
                                .content(messageNode.path("content").asText())
                                .name(messageNode.path("name").isMissingNode() ? null : messageNode.path("name").asText())
                                .build();
                        choiceBuilder.message(message);
                    }

                    choices.add(choiceBuilder.build());
                }
                responseBuilder.choices(choices);
            }

            // Parse usage
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                ChatResponse.Usage usage = ChatResponse.Usage.builder()
                        .promptTokens(usageNode.path("prompt_tokens").asInt())
                        .completionTokens(usageNode.path("completion_tokens").asInt())
                        .totalTokens(usageNode.path("total_tokens").asInt())
                        .build();
                responseBuilder.usage(usage);
            }

            // Parse system_fingerprint if present
            if (root.has("system_fingerprint")) {
                responseBuilder.systemFingerprint(root.path("system_fingerprint").asText());
            }

            return responseBuilder.build();

        } catch (Exception e) {
            log.error("解析DeepSeek响应失败: {}", responseBody, e);
            return ChatResponse.builder()
                    .error(ChatResponse.ErrorInfo.builder()
                            .type("parse_error")
                            .message("Failed to parse response: " + e.getMessage())
                            .build())
                    .build();
        }
    }

    @Override
    protected ChatResponse parseStreamChatResponse(String line) {
        // 处理流式响应的每一行
        if (line.startsWith("data: ")) {
            String data = line.substring(6);
            if ("[DONE]".equals(data)) {
                return ChatResponse.builder()
                        .object("chat.completion.chunk")
                        .build();
            }
            return parseChatResponse(data);
        }
        return null;
    }

    @Override
    protected Map<String, Object> buildEmbeddingRequestBody(EmbeddingRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : "text-embedding-3-small");
        body.put("input", request.getInput());
        
        if (request.getEncodingFormat() != null) {
            body.put("encoding_format", request.getEncodingFormat());
        }
        if (request.getDimensions() != null) {
            body.put("dimensions", request.getDimensions());
        }
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }
        
        return body;
    }

    @Override
    protected EmbeddingResponse parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            EmbeddingResponse.EmbeddingResponseBuilder builder = EmbeddingResponse.builder()
                    .object(root.path("object").asText())
                    .model(root.path("model").asText());

            // Parse usage
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                builder.promptTokens(usageNode.path("prompt_tokens").asInt());
                builder.totalTokens(usageNode.path("total_tokens").asInt());
            }

            // Parse data
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                List<EmbeddingResponse.EmbeddingData> dataList = new ArrayList<>();
                for (JsonNode item : dataNode) {
                    EmbeddingResponse.EmbeddingData.EmbeddingDataBuilder dataBuilder = EmbeddingResponse.EmbeddingData.builder()
                            .object(item.path("object").asText())
                            .index(item.path("index").asInt());

                    // Parse embedding
                    JsonNode embeddingNode = item.path("embedding");
                    if (embeddingNode.isArray()) {
                        List<Double> embedding = new ArrayList<>();
                        for (JsonNode value : embeddingNode) {
                            embedding.add(value.asDouble());
                        }
                        dataBuilder.embedding(embedding);
                    }

                    dataList.add(dataBuilder.build());
                }
                builder.data(dataList);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("解析DeepSeek Embedding响应失败: {}", responseBody, e);
            return EmbeddingResponse.builder()
                    .error("Failed to parse embedding response: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Flux<ChatResponse> streamChat(ChatRequest request) {
        Map<String, Object> requestBody = buildChatRequestBody(request);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(line -> {
                    ChatResponse response = parseStreamChatResponse(line);
                    if (response == null) {
                        response = ChatResponse.builder().build();
                    }
                    return response;
                })
                .onErrorResume(e -> {
                    log.error("DeepSeek stream chat error", e);
                    return Flux.just(ChatResponse.builder()
                            .error(ChatResponse.ErrorInfo.builder()
                                    .type("stream_error")
                                    .message(e.getMessage())
                                    .build())
                            .build());
                });
    }

    @Override
    public ModelPrice getModelPrice(String model) {
        return modelPrices.get(model);
    }
}