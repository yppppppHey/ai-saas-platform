package com.aisaas.chat.service;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatConversation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ConversationService {

    /**
     * 创建会话
     */
    ChatConversation createConversation(Long userId, ConversationCreateDTO createDTO);

    /**
     * 删除会话
     */
    void deleteConversation(Long conversationId, Long userId);

    /**
     * 更新会话
     */
    ChatConversation updateConversation(Long conversationId, Long userId, ConversationUpdateDTO updateDTO);

    /**
     * 查询会话详情
     */
    ChatConversation getConversationDetail(Long conversationId, Long userId);

    /**
     * 分页查询用户会话列表
     */
    IPage<ChatConversation> getConversationPage(Long userId, Page<ChatConversation> page, String keyword, Integer isArchived);

    /**
     * 置顶会话
     */
    void pinConversation(Long conversationId, Long userId);

    /**
     * 取消置顶会话
     */
    void unpinConversation(Long conversationId, Long userId);

    /**
     * 归档会话
     */
    void archiveConversation(Long conversationId, Long userId);

    /**
     * 取消归档会话
     */
    void unarchiveConversation(Long conversationId, Long userId);

    /**
     * 查询用户置顶会话列表
     */
    List<ChatConversation> getPinnedConversations(Long userId);

    /**
     * 查询用户非置顶会话列表
     */
    List<ChatConversation> getUnpinnedConversations(Long userId, Integer isArchived);

    /**
     * 批量删除会话
     */
    void batchDeleteConversations(List<Long> conversationIds, Long userId);

    /**
     * 清空用户所有会话
     */
    void clearAllConversations(Long userId);

    /**
     * 增加会话Token使用量
     */
    void incrementTokenUsage(Long conversationId, Long tokens);

    /**
     * 更新会话消息统计
     */
    void updateMessageStats(Long conversationId, String lastMessagePreview);
}
