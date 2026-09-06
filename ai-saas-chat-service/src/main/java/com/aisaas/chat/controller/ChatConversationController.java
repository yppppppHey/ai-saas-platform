package com.aisaas.chat.controller;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.service.ChatConversationService;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatConversationController {

    private final ChatConversationService conversationService;

    /**
     * 创建会话
     */
    @PostMapping
    public Result<Long> createConversation(@RequestAttribute("userId") Long userId,
                                           @Valid @RequestBody ConversationCreateDTO dto) {
        var conversation = conversationService.createConversation(userId, dto);
        return Result.success(conversation.getId());
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public Result<Boolean> deleteConversation(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long conversationId) {
        boolean success = conversationService.deleteConversation(userId, conversationId);
        return Result.success(success);
    }

    /**
     * 更新会话
     */
    @PutMapping("/{conversationId}")
    public Result<Boolean> updateConversation(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long conversationId,
                                            @RequestBody ConversationUpdateDTO dto) {
        conversationService.updateConversation(userId, conversationId, dto);
        return Result.success(true);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{conversationId}")
    public Result<ConversationDetailDTO> getConversationDetail(@RequestAttribute("userId") Long userId,
                                                               @PathVariable Long conversationId) {
        ConversationDetailDTO detail = conversationService.getConversationDetail(userId, conversationId);
        return Result.success(detail);
    }

    /**
     * 分页查询会话列表
     */
    @GetMapping
    public Result<Page<ConversationListDTO>> listConversations(@RequestAttribute("userId") Long userId,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<ConversationListDTO> page = conversationService.listConversations(userId, keyword, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 分页查询归档会话列表
     */
    @GetMapping("/archived")
    public Result<Page<ConversationListDTO>> listArchivedConversations(@RequestAttribute("userId") Long userId,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<ConversationListDTO> page = conversationService.listArchivedConversations(userId, keyword, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 置顶会话
     */
    @PostMapping("/{conversationId}/pin")
    public Result<Boolean> pinConversation(@RequestAttribute("userId") Long userId,
                                          @PathVariable Long conversationId) {
        boolean success = conversationService.pinConversation(userId, conversationId);
        return Result.success(success);
    }

    /**
     * 取消置顶会话
     */
    @PostMapping("/{conversationId}/unpin")
    public Result<Boolean> unpinConversation(@RequestAttribute("userId") Long userId,
                                            @PathVariable Long conversationId) {
        boolean success = conversationService.unpinConversation(userId, conversationId);
        return Result.success(success);
    }

    /**
     * 归档会话
     */
    @PostMapping("/{conversationId}/archive")
    public Result<Boolean> archiveConversation(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long conversationId) {
        boolean success = conversationService.archiveConversation(userId, conversationId);
        return Result.success(success);
    }

    /**
     * 取消归档会话
     */
    @PostMapping("/{conversationId}/unarchive")
    public Result<Boolean> unarchiveConversation(@RequestAttribute("userId") Long userId,
                                                @PathVariable Long conversationId) {
        boolean success = conversationService.unarchiveConversation(userId, conversationId);
        return Result.success(success);
    }

    /**
     * 获取置顶会话列表
     */
    @GetMapping("/pinned")
    public Result<List<ConversationListDTO>> getPinnedConversations(@RequestAttribute("userId") Long userId) {
        List<ConversationListDTO> conversations = conversationService.getPinnedConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 获取会话统计信息
     */
    @GetMapping("/stats")
    public Result<ConversationStatsDTO> getConversationStats(@RequestAttribute("userId") Long userId) {
        ConversationStatsDTO stats = conversationService.getConversationStats(userId);
        return Result.success(stats);
    }

    /**
     * 清空会话消息
     */
    @PostMapping("/{conversationId}/clear")
    public Result<Boolean> clearConversationMessages(@RequestAttribute("userId") Long userId,
                                                     @PathVariable Long conversationId) {
        boolean success = conversationService.clearConversationMessages(userId, conversationId);
        return Result.success(success);
    }
}
