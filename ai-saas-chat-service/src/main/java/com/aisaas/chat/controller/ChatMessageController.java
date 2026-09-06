package com.aisaas.chat.controller;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatMessage;
import com.aisaas.chat.service.ChatMessageService;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;

    /**
     * 发送文本消息
     */
    @PostMapping("/send")
    public Result<Long> sendTextMessage(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody MessageSendDTO dto) {
        ChatMessage message = messageService.sendTextMessage(userId, dto);
        return Result.success(message.getId());
    }

    /**
     * 发送文件消息
     */
    @PostMapping("/send-file")
    public Result<Long> sendFileMessage(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody MessageSendDTO dto) {
        ChatMessage message = messageService.sendFileMessage(userId, dto);
        return Result.success(message.getId());
    }

    /**
     * 编辑消息
     */
    @PutMapping("/{messageId}")
    public Result<Boolean> editMessage(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long messageId,
                                      @Valid @RequestBody MessageEditDTO dto) {
        messageService.editMessage(userId, messageId, dto);
        return Result.success(true);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public Result<Boolean> deleteMessage(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long messageId) {
        boolean success = messageService.deleteMessage(userId, messageId);
        return Result.success(success);
    }

    /**
     * 批量删除消息
     */
    @DeleteMapping("/batch")
    public Result<Boolean> batchDeleteMessages(@RequestAttribute("userId") Long userId,
                                              @RequestBody List<Long> messageIds) {
        boolean success = messageService.batchDeleteMessages(userId, messageIds);
        return Result.success(success);
    }

    /**
     * 重新生成消息
     */
    @PostMapping("/{messageId}/regenerate")
    public Result<Long> regenerateMessage(@RequestAttribute("userId") Long userId,
                                         @PathVariable Long messageId,
                                         @RequestBody(required = false) MessageRegenerateDTO dto) {
        if (dto == null) {
            dto = new MessageRegenerateDTO();
            dto.setMessageId(messageId);
            dto.setUseSameContext(true);
        } else {
            dto.setMessageId(messageId);
        }
        ChatMessage message = messageService.regenerateMessage(userId, dto);
        return Result.success(message.getId());
    }

    /**
     * 分页查询会话消息
     */
    @GetMapping("/conversation/{conversationId}")
    public Result<Page<MessageListDTO>> listMessages(@RequestAttribute("userId") Long userId,
                                                      @PathVariable Long conversationId,
                                                      @RequestParam(required = false) Integer messageType,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        // 这里可以添加权限校验
        Page<MessageListDTO> page = messageService.listMessages(conversationId, messageType, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取会话的所有消息
     */
    @GetMapping("/conversation/{conversationId}/all")
    public Result<List<MessageListDTO>> getConversationMessages(@RequestAttribute("userId") Long userId,
                                                                 @PathVariable Long conversationId) {
        // 这里可以添加权限校验
        List<MessageListDTO> messages = messageService.getConversationMessages(conversationId);
        return Result.success(messages);
    }

    /**
     * 获取消息详情
     */
    @GetMapping("/{messageId}")
    public Result<MessageListDTO> getMessageDetail(@RequestAttribute("userId") Long userId,
                                                    @PathVariable Long messageId) {
        MessageListDTO message = messageService.getMessageDetail(messageId);
        return Result.success(message);
    }

    /**
     * 获取消息数量
     */
    @GetMapping("/conversation/{conversationId}/count")
    public Result<Integer> getMessageCount(@RequestAttribute("userId") Long userId,
                                          @PathVariable Long conversationId) {
        Integer count = messageService.getMessageCount(conversationId);
        return Result.success(count);
    }
}
