package com.aisaas.chat.service;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatMessage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 发送文本消息
     */
    ChatMessage sendTextMessage(Long userId, MessageSendDTO sendDTO);

    /**
     * 发送文件消息
     */
    ChatMessage sendFileMessage(Long userId, Long conversationId, String fileName, String fileUrl, String fileType);

    /**
     * 编辑消息
     */
    ChatMessage editMessage(Long userId, MessageEditDTO editDTO);

    /**
     * 删除消息
     */
    void deleteMessage(Long messageId, Long userId);

    /**
     * 批量删除消息
     */
    void batchDeleteMessages(List<Long> messageIds, Long userId);

    /**
     * 重新生成AI回复
     */
    ChatMessage regenerateMessage(Long userId, MessageRegenerateDTO regenerateDTO);

    /**
     * 分页查询会话消息
     */
    IPage<ChatMessage> getMessagePage(Long conversationId, Page<ChatMessage> page, Integer messageType);

    /**
     * 查询会话的所有消息
     */
    List<ChatMessage> getMessagesByConversationId(Long conversationId, Long userId);

    /**
     * 查询消息详情
     */
    ChatMessage getMessageDetail(Long messageId, Long userId);

    /**
     * 查询会话的消息数量
     */
    Integer getMessageCount(Long conversationId, Long userId);

    /**
     * 查询会话的最后一条消息
     */
    ChatMessage getLastMessage(Long conversationId, Long userId);

    /**
     * 清除会话的所有消息
     */
    void clearConversationMessages(Long conversationId, Long userId);

    /**
     * 更新消息Token使用量
     */
    void updateTokenUsage(Long messageId, Integer inputTokens, Integer outputTokens, java.math.BigDecimal cost);

    /**
     * 更新消息生成统计
     */
    void updateGenerateStats(Long messageId, Long generateTime, Long firstTokenLatency);

    /**
     * 更新消息状态
     */
    void updateMessageStatus(Long messageId, Integer status, String errorMsg);

    /**
     * 创建AI回复消息(异步生成时使用)
     */
    ChatMessage createAiReplyMessage(Long conversationId, Long userId, Long parentMessageId, String model);
}
