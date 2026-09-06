package com.aisaas.chat.service;

import com.aisaas.chat.dto.ai.AiChatRequestDTO;
import com.aisaas.chat.dto.ai.AiChatResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * AI对话服务接口
 * 提供流式对话、上下文管理、Token统计等功能
 */
public interface AiChatService {

    /**
     * 流式对话（SSE方式）
     *
     * @param userId  用户ID
     * @param request 对话请求
     * @return SSE发射器
     */
    SseEmitter streamChat(Long userId, AiChatRequestDTO request);

    /**
     * 流式对话（Flux方式 - 用于响应式编程）
     *
     * @param userId  用户ID
     * @param request 对话请求
     * @return 响应流
     */
    Flux<AiChatResponseDTO> streamChatFlux(Long userId, AiChatRequestDTO request);

    /**
     * 同步对话（非流式）
     *
     * @param userId  用户ID
     * @param request 对话请求
     * @return 对话响应
     */
    AiChatResponseDTO chat(Long userId, AiChatRequestDTO request);

    /**
     * 构建对话上下文
     *
     * @param conversationId 会话ID
     * @param userMessage    用户当前消息
     * @param maxContext     最大上下文条数
     * @return 构建好的上下文消息列表
     */
    java.util.List<com.aisaas.common.ai.dto.ChatRequest.Message> buildContext(
            Long conversationId, String userMessage, Integer maxContext);

    /**
     * 统计Token使用量
     *
     * @param conversationId 会话ID
     * @param messageId      消息ID
     * @param inputTokens    输入Token数
     * @param outputTokens   输出Token数
     * @param model          使用的模型
     */
    void recordTokenUsage(Long conversationId, Long messageId,
                         Integer inputTokens, Integer outputTokens, String model);

    /**
     * 停止生成
     *
     * @param userId    用户ID
     * @param messageId 正在生成的消息ID
     * @return 是否成功
     */
    boolean stopGeneration(Long userId, Long messageId);

    /**
     * 获取生成状态
     *
     * @param messageId 消息ID
     * @return 状态信息
     */
    AiChatResponseDTO.GenerationStatus getGenerationStatus(Long messageId);
}