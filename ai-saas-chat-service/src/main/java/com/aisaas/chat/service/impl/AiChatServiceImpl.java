package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.ai.AiChatRequestDTO;
import com.aisaas.chat.dto.ai.AiChatResponseDTO;
import com.aisaas.chat.entity.ChatConversation;
import com.aisaas.chat.entity.ChatMessage;
import com.aisaas.chat.mapper.ChatConversationMapper;
import com.aisaas.chat.mapper.ChatMessageMapper;
import com.aisaas.chat.service.AiChatService;
import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import com.aisaas.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI对话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatMessageMapper messageMapper;

    private final com.aisaas.chat.mq.TokenUsageProducer tokenUsageProducer;

    private final com.aisaas.chat.service.ModelFailoverService modelFailoverService;
    private final ChatConversationMapper conversationMapper;
    private final AIProviderFactory aiProviderFactory;

    // 存储正在进行的生成任务（用于停止生成）
    private final Map<Long, AtomicBoolean> generationTasks = new ConcurrentHashMap<>();

    @Override
    public SseEmitter streamChat(Long userId, AiChatRequestDTO request) {
        // 创建SSE发射器，设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);

        // 异步执行对话
        CompletableFuture.runAsync(() -> {
            try {
                doStreamChat(userId, request, emitter);
            } catch (Exception e) {
                log.error("流式对话执行失败", e);
                sendErrorEvent(emitter, "INTERNAL_ERROR", "对话执行失败: " + e.getMessage());
            }
        });

        // 处理连接关闭
        emitter.onCompletion(() -> log.info("SSE连接完成: userId={}", userId));
        emitter.onTimeout(() -> log.warn("SSE连接超时: userId={}", userId));
        emitter.onError(e -> log.error("SSE连接错误: userId={}, error={}", userId, e.getMessage()));

        return emitter;
    }

    @Override
    public Flux<AiChatResponseDTO> streamChatFlux(Long userId, AiChatRequestDTO request) {
        Sinks.Many<AiChatResponseDTO> sink = Sinks.many().multicast().onBackpressureBuffer();

        CompletableFuture.runAsync(() -> {
            try {
                doStreamChatFlux(userId, request, sink);
            } catch (Exception e) {
                log.error("流式对话执行失败", e);
                sink.tryEmitNext(AiChatResponseDTO.error(null, "INTERNAL_ERROR", e.getMessage()));
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }

    @Override
    public AiChatResponseDTO chat(Long userId, AiChatRequestDTO request) {
        // 获取会话信息
        ChatConversation conversation = getAndValidateConversation(userId, request.getConversationId());

        // 保存用户消息
        ChatMessage userMessage = saveUserMessage(userId, request, conversation);

        // 获取AI Provider
        AIProvider provider = getAIProvider(request.getModel(), conversation.getProvider());
        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : conversation.getModel();

        // 构建上下文
        List<ChatRequest.Message> contextMessages = buildContextWithCurrent(
                request.getConversationId(), request.getContent(), request.getPromptTemplateId(),
                request.getTemplateVariables(), conversation.getSystemPrompt(), 10);

        // 构建请求
        ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .messages(contextMessages)
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .maxTokens(request.getMaxTokens())
                .stream(false)
                .build();

        // 调用AI（带故障降级：主模型失败自动切换备选模型）
        long startTime = System.currentTimeMillis();
        ChatResponse response = modelFailoverService.chatWithFailover(conversation.getProvider(), chatRequest);
        long totalTime = System.currentTimeMillis() - startTime;

        // 处理结果
        if (!response.isSuccess()) {
            throw new BizException("AI调用失败: " + (response.getError() != null ? response.getError().getMessage() : "未知错误"));
        }

        String aiContent = response.getContent();
        int inputTokens = response.getUsage() != null ? response.getUsage().getPromptTokens() : 0;
        int outputTokens = response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0;
        int totalTokens = response.getUsage() != null ? response.getUsage().getTotalTokens() : 0;

        // 保存AI回复
        ChatMessage aiMessage = saveAiMessage(userId, request.getConversationId(),
                userMessage.getId(), aiContent, model, provider.getProviderName(),
                inputTokens, outputTokens, totalTime);

        // 记录Token使用（降级后实际模型可能与请求不同，按响应里的真实模型计费）
        recordTokenUsage(request.getConversationId(), aiMessage.getId(), inputTokens, outputTokens,
                StringUtils.hasText(response.getModel()) ? response.getModel() : model);

        // 更新会话统计
        updateConversationStats(request.getConversationId());

        // 构建响应
        return AiChatResponseDTO.builder()
                .messageId(aiMessage.getId())
                .conversationId(request.getConversationId())
                .type("complete")
                .fullContent(aiContent)
                .done(true)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .totalTime(totalTime)
                .build();
    }

    @Override
    public java.util.List<ChatRequest.Message> buildContext(Long conversationId, String userMessage, Integer maxContext) {
        return buildContextWithCurrent(conversationId, userMessage, null, null, null, maxContext);
    }

    @Override
    public void recordTokenUsage(Long conversationId, Long messageId, Integer inputTokens, Integer outputTokens, String model) {
        // 更新消息的token使用
        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setInputTokens(inputTokens != null ? inputTokens : 0);
        message.setOutputTokens(outputTokens != null ? outputTokens : 0);
        message.setTotalTokens((inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0));
        messageMapper.updateById(message);

        // 更新会话的token统计
        conversationMapper.incrementTokenUsage(conversationId,
                (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0));

        // 跨服务计费：发 MQ 通知 billing 记账（异步、可对账，失败不影响对话主链路）
        try {
            ChatMessage sent = messageMapper.selectById(messageId);
            // 按实际使用的模型解析 Provider（降级链可能跨服务商）
            AIProvider usedProvider = aiProviderFactory.getProviderByModel(
                    StringUtils.hasText(model) ? model : "gpt-3.5-turbo");
            com.aisaas.common.mq.message.TokenUsageMessage usageMsg =
                    com.aisaas.common.mq.message.TokenUsageMessage.builder()
                            .usageId(java.util.UUID.randomUUID().toString())
                            .userId(sent != null ? sent.getUserId() : null)
                            .conversationId(conversationId)
                            .messageId(messageId)
                            .provider(usedProvider != null ? usedProvider.getProviderName() : "openai")
                            .modelId(model)
                            .operationType("chat")
                            .promptTokens(inputTokens)
                            .completionTokens(outputTokens)
                            .totalTokens((inputTokens != null ? inputTokens : 0)
                                    + (outputTokens != null ? outputTokens : 0))
                            .build();
            tokenUsageProducer.send(usageMsg);
        } catch (Exception e) {
            log.warn("发送Token用量消息失败, 等待对账补偿: conversationId={}, messageId={}",
                    conversationId, messageId, e);
        }
    }

    @Override
    public boolean stopGeneration(Long userId, Long messageId) {
        AtomicBoolean stopFlag = generationTasks.get(messageId);
        if (stopFlag != null) {
            stopFlag.set(true);
            log.info("停止生成: userId={}, messageId={}", userId, messageId);
            return true;
        }
        return false;
    }

    @Override
    public AiChatResponseDTO.GenerationStatus getGenerationStatus(Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            return null;
        }

        String status;
        switch (message.getStatus()) {
            case 0:
                status = "generating";
                break;
            case 1:
                status = "completed";
                break;
            case 2:
                status = "failed";
                break;
            case 3:
                status = "stopped";
                break;
            default:
                status = "unknown";
        }

        return AiChatResponseDTO.GenerationStatus.builder()
                .messageId(messageId)
                .status(status)
                .generatedContent(message.getContent())
                .build();
    }

    // ==================== 私有方法 ====================

    private void doStreamChat(Long userId, AiChatRequestDTO request, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        Long messageId = null;

        try {
            // 验证并获取会话
            ChatConversation conversation = getAndValidateConversation(userId, request.getConversationId());

            // 保存用户消息
            ChatMessage userMessage = saveUserMessage(userId, request, conversation);

            // 创建停止标志
            AtomicBoolean stopFlag = new AtomicBoolean(false);

            // 保存AI回复（初始状态为生成中）
            ChatMessage aiMessage = createAiMessage(userId, request.getConversationId(),
                    userMessage.getId(), conversation.getModel());
            messageId = aiMessage.getId();
            generationTasks.put(messageId, stopFlag);

            // 发送开始事件
            sendEvent(emitter, "start", AiChatResponseDTO.builder()
                    .messageId(aiMessage.getId())
                    .conversationId(request.getConversationId())
                    .type("start")
                    .build());

            // 获取AI Provider
            AIProvider provider = getAIProvider(request.getModel(), conversation.getProvider());
            String model = StringUtils.hasText(request.getModel()) ? request.getModel() : conversation.getModel();

            // 构建上下文
            List<ChatRequest.Message> contextMessages = buildContextWithCurrent(
                    request.getConversationId(), request.getContent(), request.getPromptTemplateId(),
                    request.getTemplateVariables(), conversation.getSystemPrompt(), 10);

            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .model(model)
                    .messages(contextMessages)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                    .maxTokens(request.getMaxTokens())
                    .stream(true)
                    .build();

            // 记录首字延迟
            AtomicLong firstTokenTime = new AtomicLong(0);
            AtomicBoolean firstTokenReceived = new AtomicBoolean(false);
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger inputTokens = new AtomicInteger(0);
            AtomicInteger outputTokens = new AtomicInteger(0);

            // 执行流式对话
            provider.streamChat(chatRequest)
                    .takeWhile(response -> !stopFlag.get())
                    .doOnNext(response -> {
                        if (!firstTokenReceived.get()) {
                            firstTokenTime.set(System.currentTimeMillis() - startTime);
                            firstTokenReceived.set(true);
                        }

                        String delta = extractDeltaContent(response);
                        if (StringUtils.hasText(delta)) {
                            fullContent.append(delta);
                            outputTokens.addAndGet(estimateTokens(delta));

                            // 发送增量内容
                            sendEvent(emitter, "delta", AiChatResponseDTO.builder()
                                    .messageId(aiMessage.getId())
                                    .type("delta")
                                    .content(delta)
                                    .build());
                        }

                        // 记录token使用量
                        if (response.getUsage() != null) {
                            inputTokens.set(response.getUsage().getPromptTokens());
                            outputTokens.set(response.getUsage().getCompletionTokens());
                        }
                    })
                    .doOnComplete(() -> {
                        long totalTime = System.currentTimeMillis() - startTime;

                        // 更新AI消息
                        updateAiMessage(aiMessage.getId(), fullContent.toString(), model,
                                inputTokens.get(), outputTokens.get(), totalTime, firstTokenTime.get());

                        // 记录token使用
                        recordTokenUsage(request.getConversationId(), aiMessage.getId(),
                                inputTokens.get(), outputTokens.get(), model);

                        // 更新会话统计
                        updateConversationStats(request.getConversationId());

                        // 发送完成事件
                        sendEvent(emitter, "complete", AiChatResponseDTO.builder()
                                .messageId(aiMessage.getId())
                                .conversationId(request.getConversationId())
                                .type("complete")
                                .fullContent(fullContent.toString())
                                .model(model)
                                .inputTokens(inputTokens.get())
                                .outputTokens(outputTokens.get())
                                .totalTokens(inputTokens.get() + outputTokens.get())
                                .firstTokenLatency(firstTokenTime.get())
                                .totalTime(totalTime)
                                .done(true)
                                .build());

                        // 完成SSE
                        emitter.complete();
                    })
                    .doOnError(error -> {
                        log.error("流式对话失败", error);

                        // 更新消息状态为失败
                        messageMapper.updateStatus(aiMessage.getId(), 2, error.getMessage());

                        // 发送错误事件
                        sendErrorEvent(emitter, "AI_ERROR", error.getMessage());
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("启动流式对话失败", e);
            sendErrorEvent(emitter, "START_ERROR", e.getMessage());
        }
    }

    private void doStreamChatFlux(Long userId, AiChatRequestDTO request, Sinks.Many<AiChatResponseDTO> sink) {
        // 复用doStreamChat的逻辑，但输出到sink
        SseEmitter emitter = new SseEmitter();

        // 包装emitter的事件发送到sink
        // ... 实际实现与doStreamChat类似

        doStreamChat(userId, request, emitter);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("发送SSE事件失败", e);
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String errorCode, String errorMessage) {
        try {
            AiChatResponseDTO errorResponse = AiChatResponseDTO.error(null, errorCode, errorMessage);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorResponse));
            emitter.complete();
        } catch (IOException e) {
            log.warn("发送错误事件失败", e);
        }
    }

    // ==================== 私有辅助方法 ====================

    private ChatConversation getAndValidateConversation(Long userId, Long conversationId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException("会话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BizException("无权限访问此会话");
        }
        return conversation;
    }

    private ChatMessage saveUserMessage(Long userId, AiChatRequestDTO request, ChatConversation conversation) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(request.getConversationId());
        message.setUserId(userId);
        message.setMessageType(1); // 用户消息
        message.setContentType("text");
        message.setContent(request.getContent());
        message.setReplyToId(request.getReplyToId());
        message.setStatus(1);
        messageMapper.insert(message);
        return message;
    }

    private ChatMessage saveAiMessage(Long userId, Long conversationId, Long parentId,
                                      String content, String model, String provider,
                                      int inputTokens, int outputTokens, long totalTime) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setMessageType(2); // AI回复
        message.setContentType("text");
        message.setContent(content);
        message.setParentId(parentId);
        message.setModel(model);
        message.setModelVersion(provider);
        message.setInputTokens(inputTokens);
        message.setOutputTokens(outputTokens);
        message.setTotalTokens(inputTokens + outputTokens);
        message.setGenerateTime(totalTime);
        message.setStatus(1); // 完成
        messageMapper.insert(message);
        return message;
    }

    private ChatMessage createAiMessage(Long userId, Long conversationId, Long parentId, String model) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setMessageType(2); // AI回复
        message.setContentType("text");
        message.setContent("");
        message.setParentId(parentId);
        message.setModel(model);
        message.setStatus(0); // 生成中
        messageMapper.insert(message);
        return message;
    }

    private void updateAiMessage(Long messageId, String content, String model,
                                   Integer inputTokens, Integer outputTokens,
                                   Long totalTime, Long firstTokenLatency) {
        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setContent(content);
        message.setModel(model);
        message.setInputTokens(inputTokens);
        message.setOutputTokens(outputTokens);
        message.setTotalTokens(inputTokens + outputTokens);
        message.setGenerateTime(totalTime);
        message.setFirstTokenLatency(firstTokenLatency);
        message.setStatus(1); // 完成
        messageMapper.updateById(message);
    }

    private AIProvider getAIProvider(String requestModel, String conversationProvider) {
        String providerName = StringUtils.hasText(conversationProvider) ? conversationProvider : "openai";
        String model = StringUtils.hasText(requestModel) ? requestModel : "gpt-3.5-turbo";

        AIProvider provider = aiProviderFactory.getProvider(providerName);
        if (provider == null) {
            throw new BizException("不支持的AI Provider: " + providerName);
        }

        if (!provider.supportsModel(model)) {
            log.warn("Provider {} 可能不支持模型 {}，尝试继续调用", providerName, model);
        }

        return provider;
    }

    private List<ChatRequest.Message> buildContextWithCurrent(Long conversationId, String currentMessage,
                                                                 Long promptTemplateId, Map<String, Object> templateVariables,
                                                                 String systemPrompt, Integer maxContext) {
        List<ChatRequest.Message> messages = new ArrayList<>();

        // 1. 添加系统提示词
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(ChatRequest.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build());
        }

        // 2. 如果有模板，处理模板
        if (promptTemplateId != null && templateVariables != null) {
            // 这里可以添加模板处理逻辑
            // 从数据库获取模板并渲染
        }

        // 3. 添加历史消息（最近N条）
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getStatus, 1) // 只取已完成的消息
                .orderByDesc(ChatMessage::getCreatedAt)
                .last("LIMIT " + (maxContext != null ? maxContext : 10));

        List<ChatMessage> historyMessages = messageMapper.selectList(wrapper);

        // 反转顺序，按时间正序排列
        java.util.Collections.reverse(historyMessages);

        for (ChatMessage msg : historyMessages) {
            String role = msg.getMessageType() == 1 ? "user" : "assistant";
            messages.add(ChatRequest.Message.builder()
                    .role(role)
                    .content(msg.getContent())
                    .build());
        }

        // 4. 添加当前用户消息
        messages.add(ChatRequest.Message.builder()
                .role("user")
                .content(currentMessage)
                .build());

        return messages;
    }

    private String extractDeltaContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatResponse.Choice choice = response.getChoices().get(0);
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }

    private int estimateTokens(String text) {
        // 简化的token估算
        return text == null ? 0 : text.length() / 4 + 1;
    }

    private void updateConversationStats(Long conversationId) {
        // 更新消息数量
        conversationMapper.updateMessageCount(conversationId);
        // 更新最后消息时间
        conversationMapper.updateLastMessageTime(conversationId, LocalDateTime.now());
    }
}