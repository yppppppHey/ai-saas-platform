package com.aisaas.chat.service;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatConversation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ChatConversationService extends IService<ChatConversation> {

    /**
     * 创建会话
     *
     * @param userId 用户ID
     * @param dto    创建DTO
     * @return 创建的会话
     */
    ChatConversation createConversation(Long userId, ConversationCreateDTO dto);

    /**
     * 删除会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean deleteConversation(Long userId, Long conversationId);

    /**
     * 更新会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param dto            更新DTO
     * @return 更新后的会话
     */
    ChatConversation updateConversation(Long userId, Long conversationId, ConversationUpdateDTO dto);

    /**
     * 获取会话详情
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 会话详情
     */
    ConversationDetailDTO getConversationDetail(Long userId, Long conversationId);

    /**
     * 分页查询会话列表
     *
     * @param userId     用户ID
     * @param keyword    关键词
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @return 分页结果
     */
    Page<ConversationListDTO> listConversations(Long userId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 分页查询归档会话列表
     *
     * @param userId     用户ID
     * @param keyword    关键词
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @return 分页结果
     */
    Page<ConversationListDTO> listArchivedConversations(Long userId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 置顶会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean pinConversation(Long userId, Long conversationId);

    /**
     * 取消置顶会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean unpinConversation(Long userId, Long conversationId);

    /**
     * 归档会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean archiveConversation(Long userId, Long conversationId);

    /**
     * 取消归档会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean unarchiveConversation(Long userId, Long conversationId);

    /**
     * 获取用户的置顶会话列表
     *
     * @param userId 用户ID
     * @return 置顶会话列表
     */
    List<ConversationListDTO> getPinnedConversations(Long userId);

    /**
     * 获取用户的会话数量统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    ConversationStatsDTO getConversationStats(Long userId);

    /**
     * 清空会话所有消息
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 是否成功
     */
    boolean clearConversationMessages(Long userId, Long conversationId);
}
