package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatConversation;
import com.aisaas.chat.entity.ChatMessage;
import com.aisaas.chat.mapper.ChatConversationMapper;
import com.aisaas.chat.mapper.ChatMessageMapper;
import com.aisaas.chat.service.MessageService;
import com.aisaas.common.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements MessageService {

    private final ChatMessageMapper messageMapper;
    private final ChatConversationMapper conversationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendTextMessage(Long userId, MessageSendDTO sendDTO) {
        // 验证会话归属
        ChatConversation conversation = getAndValidateConversation(sendDTO.getConversationId(), userId);

        // 创建用户消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(sendDTO.getConversationId());
        message.setUserId(userId);
        message.setMessageType(1);
        message.setContentType(StringUtils.hasText(sendDTO.getContentType()) ? sendDTO.getContentType() : "text");
        message.setContent(sendDTO.getContent());
        message.setReplyToId(sendDTO.getReplyToId());
        message.setModel(conversation.getModel());
        message.setEditStatus(0);
        message.setEditCount(0);
        message.setRegenerateCount(0);
        message.setStatus(1);
        message.setAttachments(sendDTO.getAttachments());
        message.setExtras(sendDTO.getExtras());

        messageMapper.insert(message);

        // 更新会话统计
        updateConversationStats(sendDTO.getConversationId(), sendDTO.getContent());

        log.info("发送消息成功: userId={}, conversationId={}, messageId={}", userId, sendDTO.getConversationId(), message.getId());
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendFileMessage(Long userId, Long conversationId, String fileName, String fileUrl, String fileType) {
        // 验证会话归属
        getAndValidateConversation(conversationId, userId);

        // 构建文件消息内容
        String content = String.format("[文件] %s", fileName);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setMessageType(1);
        message.setContentType("file");
        message.setContent(content);
        message.setAttachments(String.format("{\"name\":\"%s\",\"url\":\"%s\",\"type\":\"%s\"}", fileName, fileUrl, fileType));
        message.setEditStatus(0);
        message.setEditCount(0);
        message.setStatus(1);

        messageMapper.insert(message);

        // 更新会话统计
        updateConversationStats(conversationId, content);

        log.info("发送文件消息成功: userId={}, conversationId={}, messageId={}", userId, conversationId, message.getId());
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage editMessage(Long userId, MessageEditDTO editDTO) {
        ChatMessage message = getAndValidateMessage(editDTO.getMessageId(), userId);

        // 只能编辑用户消息
        if (message.getMessageType() != 1) {
            throw new BizException("只能编辑用户发送的消息");
        }

        // 保存原始内容
        if (message.getEditCount() == 0) {
            message.setOriginalContent(message.getContent());
        }

        // 更新内容
        message.setContent(editDTO.getContent());
        message.setEditStatus(1);
        message.setEditCount(message.getEditCount() + 1);
        message.setEditedAt(LocalDateTime.now());

        messageMapper.updateById(message);

        // 如果需要重新生成AI回复
        if (Boolean.TRUE.equals(editDTO.getRegenerateReply())) {
            // TODO: 触发重新生成逻辑
        }

        log.info("编辑消息成功: userId={}, messageId={}", userId, editDTO.getMessageId());
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = getAndValidateMessage(messageId, userId);

        // 软删除
        messageMapper.softDelete(messageId);

        // 更新会话统计
        Integer count = messageMapper.countByConversationId(message.getConversationId());
        ChatConversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null) {
            conversation.setMessageCount(count);
            conversationMapper.updateById(conversation);
        }

        log.info("删除消息成功: userId={}, messageId={}", userId, messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMessages(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        for (Long messageId : messageIds) {
            deleteMessage(messageId, userId);
        }
        log.info("批量删除消息成功: userId={}, count={}", userId, messageIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage regenerateMessage(Long userId, MessageRegenerateDTO regenerateDTO) {
        ChatMessage originalMessage = getAndValidateMessage(regenerateDTO.getMessageId(), userId);

        // 只能重新生成AI回复
        if (originalMessage.getMessageType() != 2) {
            throw new BizException("只能重新生成AI回复");
        }

        // 增加重新生成次数
        messageMapper.incrementRegenerateCount(regenerateDTO.getMessageId());

        // 标记当前消息为历史版本，创建新消息
        ChatMessage newMessage = new ChatMessage();
        newMessage.setConversationId(originalMessage.getConversationId());
        newMessage.setUserId(userId);
        newMessage.setMessageType(2);
        newMessage.setContentType(originalMessage.getContentType());
        newMessage.setContent(""); // 初始为空，等待流式生成
        newMessage.setParentId(originalMessage.getParentId());
        newMessage.setModel(regenerateDTO.getModel() != null ? regenerateDTO.getModel() : originalMessage.getModel());
        newMessage.setEditStatus(0);
        newMessage.setEditCount(0);
        newMessage.setRegenerateCount(0);
        newMessage.setRegenerateFromId(originalMessage.getId());
        newMessage.setStatus(0); // 生成中

        messageMapper.insert(newMessage);

        log.info("重新生成消息成功: userId={}, originalMessageId={}, newMessageId={}",
                userId, regenerateDTO.getMessageId(), newMessage.getId());
        return newMessage;
    }

    @Override
    public IPage<ChatMessage> getMessagePage(Long conversationId, Page<ChatMessage> page, Integer messageType) {
        return messageMapper.selectMessagePage(page, conversationId, messageType);
    }

    @Override
    public List<ChatMessage> getMessagesByConversationId(Long conversationId, Long userId) {
        // 验证会话归属
        getAndValidateConversation(conversationId, userId);
        return messageMapper.selectByConversationId(conversationId);
    }

    @Override
    public ChatMessage getMessageDetail(Long messageId, Long userId) {
        return getAndValidateMessage(messageId, userId);
    }

    @Override
    public Integer getMessageCount(Long conversationId, Long userId) {
        // 验证会话归属
        getAndValidateConversation(conversationId, userId);
        return messageMapper.countByConversationId(conversationId);
    }

    @Override
    public ChatMessage getLastMessage(Long conversationId, Long userId) {
        // 验证会话归属
        getAndValidateConversation(conversationId, userId);
        return messageMapper.selectLastMessage(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearConversationMessages(Long conversationId, Long userId) {
        // 验证会话归属
        getAndValidateConversation(conversationId, userId);

        // 获取所有消息并软删除
        List<ChatMessage> messages = messageMapper.selectByConversationId(conversationId);
        for (ChatMessage message : messages) {
            messageMapper.softDelete(message.getId());
        }

        // 更新会话统计
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setMessageCount(0);
            conversation.setLastMessageAt(null);
            conversation.setLastMessagePreview(null);
            conversationMapper.updateById(conversation);
        }

        log.info("清空会话消息成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTokenUsage(Long messageId, Integer inputTokens, Integer outputTokens, BigDecimal cost) {
        Integer totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
        messageMapper.updateTokenUsage(messageId, inputTokens, outputTokens, totalTokens, cost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGenerateStats(Long messageId, Long generateTime, Long firstTokenLatency) {
        messageMapper.updateGenerateStats(messageId, generateTime, firstTokenLatency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMessageStatus(Long messageId, Integer status, String errorMsg) {
        messageMapper.updateStatus(messageId, status, errorMsg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage createAiReplyMessage(Long conversationId, Long userId, Long parentMessageId, String model) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setMessageType(2);
        message.setContentType("text");
        message.setContent("");
        message.setParentId(parentMessageId);
        message.setModel(model);
        message.setEditStatus(0);
        message.setEditCount(0);
        message.setRegenerateCount(0);
        message.setStatus(0);

        messageMapper.insert(message);
        log.info("创建AI回复消息成功: conversationId={}, messageId={}", conversationId, message.getId());
        return message;
    }

    /**
     * 获取并验证会话归属
     */
    private ChatConversation getAndValidateConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || conversation.getIsDeleted() == 1) {
            throw new BizException("会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BizException("无权访问该会话");
        }
        return conversation;
    }

    /**
     * 获取并验证消息归属
     */
    private ChatMessage getAndValidateMessage(Long messageId, Long userId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null || message.getIsDeleted() == 1) {
            throw new BizException("消息不存在");
        }
        if (!userId.equals(message.getUserId())) {
            throw new BizException("无权访问该消息");
        }
        return message;
    }

    /**
     * 更新会话统计信息
     */
    private void updateConversationStats(Long conversationId, String messagePreview) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            Integer count = messageMapper.countByConversationId(conversationId);
            String preview = messagePreview;
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100);
            }
            conversationMapper.updateMessageStats(conversationId, count, LocalDateTime.now(), preview);
        }
    }
}
