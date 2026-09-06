package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatConversation;
import com.aisaas.chat.entity.ChatMessage;
import com.aisaas.chat.mapper.ChatConversationMapper;
import com.aisaas.chat.mapper.ChatMessageMapper;
import com.aisaas.chat.service.ChatConversationService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation> implements ChatConversationService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversation createConversation(Long userId, ConversationCreateDTO dto) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "新对话");
        conversation.setModel(dto.getModel());
        conversation.setProvider(dto.getProvider());
        conversation.setSystemPrompt(dto.getSystemPrompt());
        conversation.setIsPinned(0);
        conversation.setIsArchived(0);
        conversation.setMessageCount(0);
        conversation.setTokenUsage(0L);
        conversation.setStatus(1);
        conversation.setExtras(dto.getExtras());

        conversationMapper.insert(conversation);
        log.info("创建会话成功: userId={}, conversationId={}", userId, conversation.getId());
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversation(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        // 软删除会话
        conversationMapper.deleteById(conversationId);

        // 删除该会话的所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        messageMapper.delete(wrapper);

        log.info("删除会话成功: userId={}, conversationId={}", userId, conversationId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversation updateConversation(Long userId, Long conversationId, ConversationUpdateDTO dto) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        // 更新字段
        if (StringUtils.hasText(dto.getTitle())) {
            conversation.setTitle(dto.getTitle());
        }
        if (StringUtils.hasText(dto.getModel())) {
            conversation.setModel(dto.getModel());
        }
        if (StringUtils.hasText(dto.getProvider())) {
            conversation.setProvider(dto.getProvider());
        }
        if (StringUtils.hasText(dto.getSystemPrompt())) {
            conversation.setSystemPrompt(dto.getSystemPrompt());
        }
        if (dto.getIsPinned() != null) {
            conversation.setIsPinned(dto.getIsPinned());
        }
        if (dto.getIsArchived() != null) {
            conversation.setIsArchived(dto.getIsArchived());
        }
        if (dto.getStatus() != null) {
            conversation.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getExtras())) {
            conversation.setExtras(dto.getExtras());
        }

        conversationMapper.updateById(conversation);
        log.info("更新会话成功: userId={}, conversationId={}", userId, conversationId);
        return conversation;
    }

    @Override
    public ConversationDetailDTO getConversationDetail(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        return convertToDetailDTO(conversation);
    }

    @Override
    public Page<ConversationListDTO> listConversations(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Page<ChatConversation> page = new Page<>(pageNum, pageSize);
        page = conversationMapper.selectConversationPage(page, userId, keyword, 0);

        return convertToListDTOPage(page);
    }

    @Override
    public Page<ConversationListDTO> listArchivedConversations(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Page<ChatConversation> page = new Page<>(pageNum, pageSize);
        page = conversationMapper.selectConversationPage(page, userId, keyword, 1);

        return convertToListDTOPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pinConversation(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        int result = conversationMapper.updatePinnedStatus(conversationId, 1);
        log.info("置顶会话成功: userId={}, conversationId={}", userId, conversationId);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpinConversation(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        int result = conversationMapper.updatePinnedStatus(conversationId, 0);
        log.info("取消置顶会话成功: userId={}, conversationId={}", userId, conversationId);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean archiveConversation(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        // 归档前先取消置顶
        int result = conversationMapper.updateArchivedStatus(conversationId, 1);
        if (result > 0 && conversation.getIsPinned() == 1) {
            conversationMapper.updatePinnedStatus(conversationId, 0);
        }

        log.info("归档会话成功: userId={}, conversationId={}", userId, conversationId);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unarchiveConversation(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        int result = conversationMapper.updateArchivedStatus(conversationId, 0);
        log.info("取消归档会话成功: userId={}, conversationId={}", userId, conversationId);
        return result > 0;
    }

    @Override
    public List<ConversationListDTO> getPinnedConversations(Long userId) {
        List<ChatConversation> conversations = conversationMapper.selectPinnedConversations(userId);
        return conversations.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Override
    public ConversationStatsDTO getConversationStats(Long userId) {
        ConversationStatsDTO stats = new ConversationStatsDTO();

        // 总会话数
        LambdaQueryWrapper<ChatConversation> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getIsDeleted, 0);
        stats.setTotalConversations(conversationMapper.selectCount(totalWrapper).intValue());

        // 置顶会话数
        LambdaQueryWrapper<ChatConversation> pinnedWrapper = new LambdaQueryWrapper<>();
        pinnedWrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getIsPinned, 1)
                .eq(ChatConversation::getIsDeleted, 0);
        stats.setPinnedConversations(conversationMapper.selectCount(pinnedWrapper).intValue());

        // 归档会话数
        LambdaQueryWrapper<ChatConversation> archivedWrapper = new LambdaQueryWrapper<>();
        archivedWrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getIsArchived, 1)
                .eq(ChatConversation::getIsDeleted, 0);
        stats.setArchivedConversations(conversationMapper.selectCount(archivedWrapper).intValue());

        // 总消息数和Token使用量需要额外查询
        stats.setTotalMessages(messageMapper.countByUserId(userId));

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearConversationMessages(Long userId, Long conversationId) {
        ChatConversation conversation = getConversationByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BizException("会话不存在或无权限");
        }

        // 软删除该会话的所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        messageMapper.delete(wrapper);

        // 重置会话统计
        conversationMapper.updateMessageStats(conversationId, 0, null, null);

        log.info("清空会话消息成功: userId={}, conversationId={}", userId, conversationId);
        return true;
    }

    /**
     * 根据ID和用户ID获取会话
     */
    private ChatConversation getConversationByIdAndUserId(Long conversationId, Long userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getId, conversationId)
                .eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getIsDeleted, 0);
        return conversationMapper.selectOne(wrapper);
    }

    /**
     * 转换为列表DTO
     */
    private ConversationListDTO convertToListDTO(ChatConversation conversation) {
        ConversationListDTO dto = new ConversationListDTO();
        BeanUtils.copyProperties(conversation, dto);
        return dto;
    }

    /**
     * 转换为详情DTO
     */
    private ConversationDetailDTO convertToDetailDTO(ChatConversation conversation) {
        ConversationDetailDTO dto = new ConversationDetailDTO();
        BeanUtils.copyProperties(conversation, dto);
        return dto;
    }

    /**
     * 分页转换为列表DTO
     */
    private Page<ConversationListDTO> convertToListDTOPage(Page<ChatConversation> page) {
        Page<ConversationListDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ConversationListDTO> records = page.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
        resultPage.setRecords(records);
        return resultPage;
    }
}
