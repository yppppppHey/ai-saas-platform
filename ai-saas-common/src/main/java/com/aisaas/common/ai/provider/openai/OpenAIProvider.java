package com.aisaas.common.ai.provider.openai;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import com.aisaas.common.ai.provider.AbstractAIProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * OpenAI Provider 实现
 * 支持 GPT-4、GPT-3.5-Turbo 等模型
 */
@Slf4j
@Component
public class OpenAIProvider extends AbstractAIProvider {

    private static final String PROVIDER_NAME = "openai";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String[] SUPPORTED_MODELS = {
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-4-turbo-preview",
            "gpt-4",
            "gpt-4-32k",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",
            "text-embedding-3-small",
            "text-embedding-3-large",
            "text-embedding-ada-002"
    };

    public OpenAIProvider() {
        super(
                System.getenv().getOrDefault("OPENAI_API_KEY", ""),
                System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1")
        );
    }

    public OpenAIProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }

    @Override
    protected void initModelConfigs() {
        // GPT-4o 系列
        addModelConfig("gpt-4o", 128000, 5.0, 15.0);
        addModelConfig("gpt-4o-mini", 128000, 0.15, 0.60);

        // GPT-4 Turbo
        addModelConfig("gpt-4-turbo", 128000, 10.0, 30.0);
        addModelConfig("gpt-4-turbo-preview", 128000, 10.0, 30.0);

        // GPT-4
        addModelConfig("gpt-4", 8192, 30.0, 60.0);
        addModelConfig("gpt-4-32k", 32768, 60.0, 120.0);

        // GPT-3.5 Turbo
        addModelConfig("gpt-3.5-turbo", 4096, 0.5, 1.5);
        addModelConfig("gpt-3.5-turbo-16k", 16384, 1.0, 2.0);

        // Embedding Models
        addModelConfig("text-embedding-3-small", 8191, 0.02, 0.0);
        addModelConfig("text-embedding-3-large", 8191, 0.13, 0.0);
        addModelConfig("text-embedding-ada-002", 8191, 0.10, 0.0);
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
                    if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                        messageMap.put("tool_calls", msg.getToolCalls().stream()
                                .map(tc -> {
                                    Map<String, Object> tcMap = new HashMap<>();
                                    tcMap.put("id", tc.getId());
                                    tcMap.put("type", tc.getType());
                                    if (tc.getFunction() != null) {
                                        Map<String, Object> fnMap = new HashMap<>();
                                        fnMap.put("name", tc.getFunction().getName());
                                        fnMap.put("arguments", tc.getFunction().getArguments());
                                        tcMap.put("function", fnMap);
                                    }
                                    return tcMap;
                                })
                                .collect(Collectors.toList()));
                    }
                    if (msg.getToolCallId() != null) {
                        messageMap.put("tool_call_id", msg.getToolCallId());
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

        // 工具（函数调用）
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools().stream()
                    .map(tool -> {
                        Map<String, Object> toolMap = new HashMap<>();
                        toolMap.put("type", tool.getType());
                        if (tool.getFunction() != null) {
                            Map<String, Object> fnMap = new HashMap<>();
                            fnMap.put("name", tool.getFunction().getName());
                            fnMap.put("description", tool.getFunction().getDescription());
                            fnMap.put("parameters", tool.getFunction().getParameters());
                            toolMap.put("function", fnMap);
                        }
                        return toolMap;
                    })
                    .collect(Collectors.toList()));
        }

        // 工具选择策略
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }

        // 响应格式
        if (request.getResponseFormat() != null) {
            Map<String, Object> rfMap = new HashMap<>();
            rfMap.put("type", request.getResponseFormat().getType());
            if (request.getResponseFormat().getJsonSchema() != null) {
                rfMap.put("json_schema", request.getResponseFormat().getJsonSchema());
            }
            body.put("response_format", rfMap);
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
            ChatResponse.ChatResponseBuilder responseBuilder = ChatResponse.builder();

            responseBuilder.id(root.path("id").asText());
            responseBuilder.object(root.path("object").asText());
            responseBuilder.created(root.path("created").asLong());
            responseBuilder.model(root.path("model").asText());
            responseBuilder.systemFingerprint(root.path("system_fingerprint").asText());

            // 解析choices
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isArray()) {
                List<ChatResponse.Choice> choices = StreamSupport.stream(choicesNode.spliterator(), false)
                        .map(choiceNode -> {
                            ChatResponse.Choice.ChoiceBuilder choiceBuilder = ChatResponse.Choice.builder();
                            choiceBuilder.index(choiceNode.path("index").asInt());
                            choiceBuilder.finishReason(choiceNode.path("finish_reason").asText());

                            // 解析message
                            JsonNode messageNode = choiceNode.path("message");
                            if (!messageNode.isMissingNode()) {
                                ChatResponse.Message.MessageBuilder msgBuilder = ChatResponse.Message.builder();
                                msgBuilder.role(messageNode.path("role").asText());
                                msgBuilder.content(messageNode.path("content").asText());

                                // 解析tool_calls
                                JsonNode toolCallsNode = messageNode.path("tool_calls");
                                if (toolCallsNode.isArray()) {
                                    List<ChatRequest.ToolCall> toolCalls = StreamSupport.stream(toolCallsNode.spliterator(), false)
                                            .map(tcNode -> {
                                                ChatRequest.ToolCall.ToolCallBuilder tcBuilder = ChatRequest.ToolCall.builder();
                                                tcBuilder.id(tcNode.path("id").asText());
                                                tcBuilder.type(tcNode.path("type").asText());

                                                JsonNode fnNode = tcNode.path("function");
                                                if (!fnNode.isMissingNode()) {
                                                    tcBuilder.function(ChatRequest.FunctionCall.builder()
                                                            .name(fnNode.path("name").asText())
                                                            .arguments(fnNode.path("arguments").asText())
                                                            .build());
                                                }
                                                return tcBuilder.build();
                                            })
                                            .collect(Collectors.toList());
                                    msgBuilder.toolCalls(toolCalls);
                                }

                                choiceBuilder.message(msgBuilder.build());
                            }

                            return choiceBuilder.build();
                        })
                        .collect(Collectors.toList());
                responseBuilder.choices(choices);
            }

            // 解析usage
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                responseBuilder.usage(ChatResponse.Usage.builder()
                        .promptTokens(usageNode.path("prompt_tokens").asInt())
                        .completionTokens(usageNode.path("completion_tokens").asInt())
                        .totalTokens(usageNode.path("total_tokens").asInt())
                        .build());
            }

            responseBuilder.rawResponse(responseBody);
            return responseBuilder.build();

        } catch (Exception e) {
            log.error("Failed to parse chat response: {}", responseBody, e);
            return ChatResponse.builder()
                    .error(ChatResponse.ErrorInfo.builder()
                            .code("PARSE_ERROR")
                            .message("Failed to parse response: " + e.getMessage())
                            .build())
                    .build();
        }
    }

    @Override
    protected ChatResponse parseStreamChatResponse(String line) {
        try {
            JsonNode root = objectMapper.readTree(line);
            ChatResponse.ChatResponseBuilder responseBuilder = ChatResponse.builder();

            responseBuilder.id(root.path("id").asText());
            responseBuilder.object(root.path("object").asText());
            responseBuilder.created(root.path("created").asLong());
            responseBuilder.model(root.path("model").asText());

            // 解析choices
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode choiceNode = choicesNode.get(0);

                ChatResponse.Choice.ChoiceBuilder choiceBuilder = ChatResponse.Choice.builder();
                choiceBuilder.index(choiceNode.path("index").asInt());
                choiceBuilder.finishReason(choiceNode.path("finish_reason").asText(null));

                // 解析delta（流式响应使用delta而不是message）
                JsonNode deltaNode = choiceNode.path("delta");
                if (!deltaNode.isMissingNode()) {
                    ChatResponse.Message.MessageBuilder msgBuilder = ChatResponse.Message.builder();
                    msgBuilder.role(deltaNode.path("role").asText(null));
                    msgBuilder.content(deltaNode.path("content").asText(null));

                    // 解析tool_calls
                    JsonNode toolCallsNode = deltaNode.path("tool_calls");
                    if (toolCallsNode.isArray()) {
                        List<ChatRequest.ToolCall> toolCalls = StreamSupport.stream(toolCallsNode.spliterator(), false)
                                .map(tcNode -> ChatRequest.ToolCall.builder()
                                        .id(tcNode.path("id").asText())
                                        .type(tcNode.path("type").asText())
                                        .function(ChatRequest.FunctionCall.builder()
                                                .name(tcNode.path("function").path("name").asText())
                                                .arguments(tcNode.path("function").path("arguments").asText())
                                                .build())
                                        .build())
                                .collect(Collectors.toList());
                        msgBuilder.toolCalls(toolCalls);
                    }

                    choiceBuilder.message(msgBuilder.build());
                }

                responseBuilder.choices(Collections.singletonList(choiceBuilder.build()));
            }

            // 解析usage（只在最后一条消息中有）
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode() && !usageNode.isNull()) {
                responseBuilder.usage(ChatResponse.Usage.builder()
                        .promptTokens(usageNode.path("prompt_tokens").asInt())
                        .completionTokens(usageNode.path("completion_tokens").asInt())
                        .totalTokens(usageNode.path("total_tokens").asInt())
                        .build());
            }

            return responseBuilder.build();

        } catch (Exception e) {
            log.error("Failed to parse stream chat response: {}", line, e);
            return null;
        }
    }

    @Override
    protected Map<String, Object> buildEmbeddingRequestBody(EmbeddingRequest request) {
        Map<String, Object> body = new HashMap<>();

        // 模型
        body.put("model", request.getModel());

        // 输入
        if (request.isBatch()) {
            body.put("input", request.getInputs());
        } else {
            body.put("input", request.getInput());
        }

        // 编码格式
        if (request.getEncodingFormat() != null) {
            body.put("encoding_format", request.getEncodingFormat());
        }

        // 维度
        if (request.getDimensions() != null) {
            body.put("dimensions", request.getDimensions());
        }

        // 用户标识
        if (request.getUser() != null) {
            body.put("user", request.getUser());
        }

        return body;
    }

    @Override
    protected EmbeddingResponse parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            EmbeddingResponse.EmbeddingResponseBuilder responseBuilder = EmbeddingResponse.builder();

            responseBuilder.id(root.path("id").asText());
            responseBuilder.object(root.path("object").asText());
            responseBuilder.model(root.path("model").asText());

            // 解析data
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                List<EmbeddingResponse.Embedding> embeddings = StreamSupport.stream(dataNode.spliterator(), false)
                        .map(embNode -> {
                            EmbeddingResponse.Embedding.EmbeddingBuilder embBuilder = EmbeddingResponse.Embedding.builder();
                            embBuilder.index(embNode.path("index").asInt());
                            embBuilder.object(embNode.path("object").asText());

                            // 解析embedding数组
                            JsonNode embeddingNode = embNode.path("embedding");
                            if (embeddingNode.isArray()) {
                                List<Double> vector = StreamSupport.stream(embeddingNode.spliterator(), false)
                                        .map(JsonNode::asDouble)
                                        .collect(Collectors.toList());
                                embBuilder.embedding(vector);
                            }

                            return embBuilder.build();
                        })
                        .collect(Collectors.toList());
                responseBuilder.data(embeddings);

                // 设置向量维度
                if (!embeddings.isEmpty() && embeddings.get(0).getEmbedding() != null) {
                    responseBuilder.dimension(embeddings.get(0).getEmbedding().size());
                }
            }

            // 解析usage
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                responseBuilder.usage(EmbeddingResponse.Usage.builder()
                        .promptTokens(usageNode.path("prompt_tokens").asInt())
                        .totalTokens(usageNode.path("total_tokens").asInt())
                        .build());
            }

            return responseBuilder.build();

        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", responseBody, e);
            return EmbeddingResponse.builder()
                    .error(EmbeddingResponse.ErrorInfo.builder()
                            .code("PARSE_ERROR")
                            .message("Failed to parse response: " + e.getMessage())
                            .build())
                    .build();
        }
    }
}
