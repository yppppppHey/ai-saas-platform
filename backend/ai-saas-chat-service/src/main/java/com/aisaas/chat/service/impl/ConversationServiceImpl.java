package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.ConversationCreateDTO;
import com.aisaas.chat.dto.ConversationUpdateDTO;
import com.aisaas.chat.entity.ChatConversation;
import com.aisaas.chat.mapper.ChatConversationMapper;
import com.aisaas.chat.service.ConversationService;
import com.aisaas.common.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation> implements ConversationService {

    private final ChatConversationMapper conversationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversation createConversation(Long userId, ConversationCreateDTO createDTO) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(createDTO.getTitle());
        conversation.setModel(createDTO.getModel());
        conversation.setProvider(createDTO.getProvider());
        conversation.setSystemPrompt(createDTO.getSystemPrompt());
        conversation.setIsPinned(0);
        conversation.setIsArchived(0);
        conversation.setMessageCount(0);
        conversation.setTokenUsage(0L);
        conversation.setStatus(1);
        conversation.setExtras(createDTO.getExtras());

        conversationMapper.insert(conversation);
        log.info("创建会话成功: userId={}, conversationId={}", userId, conversation.getId());
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限删除");
        }
        conversationMapper.deleteById(conversationId);
        log.info("删除会话成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversation updateConversation(Long conversationId, Long userId, ConversationUpdateDTO updateDTO) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限修改");
        }

        if (StringUtils.hasText(updateDTO.getTitle())) {
            conversation.setTitle(updateDTO.getTitle());
        }
        if (StringUtils.hasText(updateDTO.getModel())) {
            conversation.setModel(updateDTO.getModel());
        }
        if (StringUtils.hasText(updateDTO.getProvider())) {
            conversation.setProvider(updateDTO.getProvider());
        }
        if (StringUtils.hasText(updateDTO.getSystemPrompt())) {
            conversation.setSystemPrompt(updateDTO.getSystemPrompt());
        }
        if (updateDTO.getStatus() != null) {
            conversation.setStatus(updateDTO.getStatus());
        }
        if (StringUtils.hasText(updateDTO.getExtras())) {
            conversation.setExtras(updateDTO.getExtras());
        }

        conversationMapper.updateById(conversation);
        log.info("更新会话成功: userId={}, conversationId={}", userId, conversationId);
        return conversation;
    }

    @Override
    public ChatConversation getConversationDetail(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限查看");
        }
        return conversation;
    }

    @Override
    public IPage<ChatConversation> getConversationPage(Long userId, Page<ChatConversation> page, String keyword, Integer isArchived) {
        return conversationMapper.selectConversationPage(page, userId, keyword, isArchived);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限操作");
        }
        if (conversation.getIsPinned() == 1) {
            return;
        }
        conversationMapper.updatePinnedStatus(conversationId, 1);
        log.info("置顶会话成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限操作");
        }
        if (conversation.getIsPinned() == 0) {
            return;
        }
        conversationMapper.updatePinnedStatus(conversationId, 0);
        log.info("取消置顶会话成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限操作");
        }
        if (conversation.getIsArchived() == 1) {
            return;
        }
        conversationMapper.updateArchivedStatus(conversationId, 1);
        log.info("归档会话成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unarchiveConversation(Long conversationId, Long userId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限操作");
        }
        if (conversation.getIsArchived() == 0) {
            return;
        }
        conversationMapper.updateArchivedStatus(conversationId, 0);
        log.info("取消归档会话成功: userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    public List<ChatConversation> getPinnedConversations(Long userId) {
        return conversationMapper.selectPinnedConversations(userId);
    }

    @Override
    public List<ChatConversation> getUnpinnedConversations(Long userId, Integer isArchived) {
        return conversationMapper.selectUnpinnedConversations(userId, isArchived);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteConversations(List<Long> conversationIds, Long userId) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return;
        }
        for (Long conversationId : conversationIds) {
            deleteConversation(conversationId, userId);
        }
        log.info("批量删除会话成功: userId={}, count={}", userId, conversationIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllConversations(Long userId) {
        // 获取用户的所有会话
        List<ChatConversation> conversations = conversationMapper.selectPinnedConversations(userId);
        conversations.addAll(conversationMapper.selectUnpinnedConversations(userId, 0));
        conversations.addAll(conversationMapper.selectUnpinnedConversations(userId, 1));

        for (ChatConversation conversation : conversations) {
            deleteConversation(conversation.getId(), userId);
        }
        log.info("清空所有会话成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementTokenUsage(Long conversationId, Long tokens) {
        conversationMapper.incrementTokenUsage(conversationId, tokens);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMessageStats(Long conversationId, String lastMessagePreview) {
        // 获取消息数量
        Integer messageCount = conversationMapper.selectById(conversationId).getMessageCount() + 1;
        conversationMapper.updateMessageStats(conversationId, messageCount, LocalDateTime.now(), lastMessagePreview);
    }

    /**
     * 根据ID和用户ID获取会话
     */
    private ChatConversation getConversationByIdAndUserId(Long conversationId, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || conversation.getIsDeleted() == 1) {
            return null;
        }
        if (!userId.equals(conversation.getUserId())) {
            return null;
        }
        return conversation;
    }
}
