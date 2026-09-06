package com.aisaas.chat.mapper;

import com.aisaas.chat.entity.ChatConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 会话Mapper
 */
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 分页查询用户会话列表
     */
    Page<ChatConversation> selectConversationPage(Page<ChatConversation> page,
                                                   @Param("userId") Long userId,
                                                   @Param("keyword") String keyword,
                                                   @Param("isArchived") Integer isArchived);

    /**
     * 查询置顶会话列表
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} AND is_pinned = 1 AND is_deleted = 0 ORDER BY updated_at DESC")
    List<ChatConversation> selectPinnedConversations(@Param("userId") Long userId);

    /**
     * 查询非置顶会话列表
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} AND is_pinned = 0 AND is_archived = #{isArchived} AND is_deleted = 0 ORDER BY last_message_at DESC")
    List<ChatConversation> selectUnpinnedConversations(@Param("userId") Long userId, @Param("isArchived") Integer isArchived);

    /**
     * 更新置顶状态
     */
    @Update("UPDATE chat_conversation SET is_pinned = #{isPinned}, updated_at = NOW() WHERE id = #{id}")
    int updatePinnedStatus(@Param("id") Long id, @Param("isPinned") Integer isPinned);

    /**
     * 更新归档状态
     */
    @Update("UPDATE chat_conversation SET is_archived = #{isArchived}, updated_at = NOW() WHERE id = #{id}")
    int updateArchivedStatus(@Param("id") Long id, @Param("isArchived") Integer isArchived);

    /**
     * 更新消息统计
     */
    @Update("UPDATE chat_conversation SET message_count = #{messageCount}, last_message_at = #{lastMessageAt}, " +
            "last_message_preview = #{lastMessagePreview}, updated_at = NOW() WHERE id = #{id}")
    int updateMessageStats(@Param("id") Long id,
                          @Param("messageCount") Integer messageCount,
                          @Param("lastMessageAt") LocalDateTime lastMessageAt,
                          @Param("lastMessagePreview") String lastMessagePreview);

    /**
     * 增加Token使用量
     */
    @Update("UPDATE chat_conversation SET token_usage = token_usage + #{tokens}, updated_at = NOW() WHERE id = #{id}")
    int incrementTokenUsage(@Param("id") Long id, @Param("tokens") Long tokens);

    /**
     * 查询会话详情
     */
    @Select("SELECT * FROM chat_conversation WHERE id = #{id} AND is_deleted = 0")
    ChatConversation selectByIdWithDeleted(@Param("id") Long id);
    
    /**
     * 更新会话消息数量
     */
    @Update("UPDATE chat_conversation SET message_count = (SELECT COUNT(*) FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0), updated_at = NOW() WHERE id = #{conversationId}")
    int updateMessageCount(@Param("conversationId") Long conversationId);

    /**
     * 更新最后消息时间
     */
    @Update("UPDATE chat_conversation SET last_message_at = #{lastMessageAt}, updated_at = NOW() WHERE id = #{conversationId}")
    int updateLastMessageTime(@Param("conversationId") Long conversationId, @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 更新最后消息预览
     */
    @Update("UPDATE chat_conversation SET last_message_preview = #{preview}, updated_at = NOW() WHERE id = #{conversationId}")
    int updateLastMessagePreview(@Param("conversationId") Long conversationId, @Param("preview") String preview);

    /**
     * 增加Token使用量
     */
    @Update("UPDATE chat_conversation SET token_usage = token_usage + #{tokens}, updated_at = NOW() WHERE id = #{conversationId}")
    int incrementTokenUsage(@Param("conversationId") Long conversationId, @Param("tokens") Integer tokens);

    /**
     * 置顶会话
     */
    @Update("UPDATE chat_conversation SET is_pinned = 1, updated_at = NOW() WHERE id = #{conversationId}")
    int pinConversation(@Param("conversationId") Long conversationId);

    /**
     * 取消置顶会话
     */
    @Update("UPDATE chat_conversation SET is_pinned = 0, updated_at = NOW() WHERE id = #{conversationId}")
    int unpinConversation(@Param("conversationId") Long conversationId);

    /**
     * 归档会话
     */
    @Update("UPDATE chat_conversation SET is_archived = 1, updated_at = NOW() WHERE id = #{conversationId}")
    int archiveConversation(@Param("conversationId") Long conversationId);

    /**
     * 取消归档会话
     */
    @Update("UPDATE chat_conversation SET is_archived = 0, updated_at = NOW() WHERE id = #{conversationId}")
    int unarchiveConversation(@Param("conversationId") Long conversationId);
}