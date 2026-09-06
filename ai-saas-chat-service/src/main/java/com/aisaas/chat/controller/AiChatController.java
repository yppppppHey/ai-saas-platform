package com.aisaas.chat.controller;

import com.aisaas.chat.dto.ai.*;
import com.aisaas.chat.service.AiChatService;
import com.aisaas.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话控制器
 * 提供流式对话、同步对话、上下文管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 流式对话（SSE）
     * 用于实时显示AI生成内容
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestAttribute("userId") Long userId,
                                 @Valid @RequestBody AiChatStreamRequestDTO request) {
        log.info("流式对话请求: userId={}, conversationId={}", userId, request.getConversationId());

        AiChatRequestDTO aiRequest = new AiChatRequestDTO();
        org.springframework.beans.BeanUtils.copyProperties(request, aiRequest);
        aiRequest.setStream(true);

        return aiChatService.streamChat(userId, aiRequest);
    }

    /**
     * 同步对话（非流式）
     * 等待完整响应后返回
     */
    @PostMapping("/chat")
    public Result<AiChatResponseDTO> chat(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody AiChatSyncRequestDTO request) {
        log.info("同步对话请求: userId={}, conversationId={}", userId, request.getConversationId());

        AiChatRequestDTO aiRequest = new AiChatRequestDTO();
        org.springframework.beans.BeanUtils.copyProperties(request, aiRequest);
        aiRequest.setStream(false);

        AiChatResponseDTO response = aiChatService.chat(userId, aiRequest);
        return Result.success(response);
    }

    /**
     * 停止生成
     * 用于用户主动取消正在进行的生成
     */
    @PostMapping("/stop")
    public Result<Boolean> stopGeneration(@RequestAttribute("userId") Long userId,
                                          @Valid @RequestBody StopGenerationRequestDTO request) {
        log.info("停止生成请求: userId={}, messageId={}", userId, request.getMessageId());
        boolean success = aiChatService.stopGeneration(userId, request.getMessageId());
        return Result.success(success);
    }

    /**
     * 获取生成状态
     * 用于轮询检查正在进行的生成任务状态
     */
    @GetMapping("/status/{messageId}")
    public Result<AiChatResponseDTO.GenerationStatus> getGenerationStatus(@RequestAttribute("userId") Long userId,
                                                                          @PathVariable Long messageId) {
        AiChatResponseDTO.GenerationStatus status = aiChatService.getGenerationStatus(messageId);
        return Result.success(status);
    }

    /**
     * 获取对话上下文
     * 返回用于AI对话的上下文消息列表
     */
    @GetMapping("/context/{conversationId}")
    public Result<java.util.List<com.aisaas.common.ai.dto.ChatRequest.Message>> getContext(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "10") Integer maxContext) {
        // 验证权限
        java.util.List<com.aisaas.common.ai.dto.ChatRequest.Message> context =
                aiChatService.buildContext(conversationId, "", maxContext);
        return Result.success(context);
    }
}