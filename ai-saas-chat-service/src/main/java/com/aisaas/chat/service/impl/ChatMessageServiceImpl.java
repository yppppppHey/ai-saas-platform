package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatConversation;
import com.aisaas.chat.entity.ChatMessage;
import com.aisaas.chat.mapper.ChatConversationMapper;
import com.aisaas.chat.mapper.ChatMessageMapper;
import com.aisaas.chat.service.AiChatService;
import com.aisaas.chat.service.ChatMessageService;
import com.aisaas.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    private final ChatMessageMapper messageMapper;
    private final ChatConversationMapper conversationMapper;
    private final AiChatService aiChatService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendTextMessage(Long userId, MessageSendDTO dto) {
        // 验证会话存在且属于当前用户
        ChatConversation conversation = getConversationById(dto.getConversationId());
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BizException("会话不存在或无权限");
        }

        // 创建用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversationId(dto.getConversationId());
        userMessage.setUserId(userId);
        userMessage.setMessageType(1); // 用户消息
        userMessage.setContentType(StringUtils.hasText(dto.getContentType()) ? dto.getContentType() : "text");
        userMessage.setContent(dto.getContent());
        userMessage.setReplyToId(dto.getReplyToId());
        userMessage.setStatus(1); // 完成
        userMessage.setAttachments(dto.getAttachments());
        userMessage.setExtras(dto.getExtras());

        messageMapper.insert(userMessage);

        // 更新会话统计
        updateConversationStats(conversation.getId());

        log.info("发送消息成功: userId={}, conversationId={}, messageId={}",
                userId, dto.getConversationId(), userMessage.getId());

        return userMessage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendFileMessage(Long userId, MessageSendDTO dto) {
        // 设置内容为文件类型
        dto.setContentType("file");
        return sendTextMessage(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage editMessage(Long userId, Long messageId, MessageEditDTO dto) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BizException("消息不存在或无权限");
        }

        // 只有用户消息可以编辑，且不能编辑已删除的消息
        if (message.getMessageType() != 1) {
            throw new BizException("只能编辑用户消息");
        }
        if (message.getEditStatus() != null && message.getEditStatus() == 2) {
            throw new BizException("消息已删除，无法编辑");
        }

        // 保存原始内容
        String originalContent = StringUtils.hasText(message.getOriginalContent())
                ? message.getOriginalContent()
                : message.getContent();

        // 更新消息
        messageMapper.updateEditStatus(messageId, dto.getContent(), originalContent, 1);

        // 如果需要重新生成AI回复
        if (Boolean.TRUE.equals(dto.getRegenerateReply())) {
            // 查找这条消息对应的AI回复
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getParentId, messageId)
                    .eq(ChatMessage::getMessageType, 2);
            ChatMessage aiReply = messageMapper.selectOne(wrapper);

            if (aiReply != null) {
                try {
                    aiChatService.regenerateReply(userId, message.getConversationId(), messageId,
                            dto.getContent(), null);
                } catch (Exception e) {
                    log.warn("重新生成AI回复失败(不影响编辑结果): messageId={}", messageId, e);
                }
            }
        }

        ChatMessage updatedMessage = messageMapper.selectById(messageId);
        log.info("编辑消息成功: userId={}, messageId={}", userId, messageId);
        return updatedMessage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long userId, Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BizException("消息不存在或无权限");
        }

        // 软删除
        messageMapper.softDelete(messageId);

        // 更新会话统计
        updateConversationStats(message.getConversationId());

        log.info("删除消息成功: userId={}, messageId={}", userId, messageId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteMessages(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return true;
        }

        for (Long messageId : messageIds) {
            try {
                deleteMessage(userId, messageId);
            } catch (Exception e) {
                log.warn("批量删除消息失败: messageId={}, error={}", messageId, e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage regenerateMessage(Long userId, MessageRegenerateDTO dto) {
        ChatMessage originalMessage = messageMapper.selectById(dto.getMessageId());
        if (originalMessage == null) {
            throw new BizException("消息不存在");
        }

        // 检查权限
        ChatConversation conversation = getConversationById(originalMessage.getConversationId());
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BizException("无权限重新生成此消息");
        }

        // 只能重新生成AI回复
        if (originalMessage.getMessageType() != 2) {
            throw new BizException("只能重新生成AI回复");
        }

        // 增加重新生成次数
        messageMapper.incrementRegenerateCount(dto.getMessageId());

        // 创建新的消息记录（这里可以触发实际的重新生成逻辑）
        ChatMessage newMessage = new ChatMessage();
        newMessage.setConversationId(originalMessage.getConversationId());
        newMessage.setUserId(userId);
        newMessage.setMessageType(2); // AI回复
        newMessage.setContentType(originalMessage.getContentType());
        newMessage.setContent("重新生成中..."); // 临时内容
        newMessage.setParentId(originalMessage.getParentId());
        newMessage.setReplyToId(originalMessage.getReplyToId());
        newMessage.setModel(StringUtils.hasText(dto.getModel()) ? dto.getModel() : originalMessage.getModel());
        newMessage.setRegenerateFromId(originalMessage.getId());
        newMessage.setStatus(0); // 生成中

        messageMapper.insert(newMessage);

        log.info("重新生成消息: userId={}, originalMessageId={}, newMessageId={}",
                userId, dto.getMessageId(), newMessage.getId());

        return newMessage;
    }

    @Override
    public Page<MessageListDTO> listMessages(Long conversationId, Integer messageType, Integer pageNum, Integer pageSize) {
        Page<ChatMessage> page = new Page<>(pageNum, pageSize);
        page = messageMapper.selectMessagePage(page, conversationId, messageType);

        return convertToMessageListDTOPage(page);
    }

    @Override
    public List<MessageListDTO> getConversationMessages(Long conversationId) {
        List<ChatMessage> messages = messageMapper.selectByConversationId(conversationId);
        return messages.stream()
                .map(this::convertToMessageListDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MessageListDTO getMessageDetail(Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            return null;
        }
        return convertToMessageListDTO(message);
    }

    @Override
    public Integer getMessageCount(Long conversationId) {
        return messageMapper.countByConversationId(conversationId);
    }

    @Override
    public List<ChatMessage> getUserMessages(Long userId) {
        return messageMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMessageTokenUsage(Long messageId, Integer inputTokens, Integer outputTokens, BigDecimal cost) {
        return messageMapper.updateTokenUsage(messageId, inputTokens, outputTokens, inputTokens + outputTokens, cost) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMessageGenerateStats(Long messageId, Long generateTime, Long firstTokenLatency) {
        return messageMapper.updateGenerateStats(messageId, generateTime, firstTokenLatency) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMessageStatus(Long messageId, Integer status, String errorMsg) {
        return messageMapper.updateStatus(messageId, status, errorMsg) > 0;
    }

    /**
     * 根据ID获取会话
     */
    private ChatConversation getConversationById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 更新会话统计信息
     */
    private void updateConversationStats(Long conversationId) {
        Integer messageCount = messageMapper.countByConversationId(conversationId);
        ChatMessage lastMessage = messageMapper.selectLastMessage(conversationId);

        String preview = lastMessage != null && lastMessage.getContent() != null
                ? lastMessage.getContent().length() > 100
                ? lastMessage.getContent().substring(0, 100) + "..."
                : lastMessage.getContent()
                : null;

        conversationMapper.updateMessageStats(conversationId, messageCount,
                lastMessage != null ? lastMessage.getCreatedAt() : null, preview);
    }

    /**
     * 转换为消息列表DTO
     */
    private MessageListDTO convertToMessageListDTO(ChatMessage message) {
        MessageListDTO dto = new MessageListDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }

    /**
     * 分页转换为消息列表DTO
     */
    private Page<MessageListDTO> convertToMessageListDTOPage(Page<ChatMessage> page) {
        Page<MessageListDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<MessageListDTO> records = page.getRecords().stream()
                .map(this::convertToMessageListDTO)
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        return resultPage;
    }
}
