package com.aisaas.chat.mapper;

import com.aisaas.chat.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 消息Mapper
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 分页查询会话消息
     */
    Page<ChatMessage> selectMessagePage(Page<ChatMessage> page,
                                          @Param("conversationId") Long conversationId,
                                          @Param("messageType") Integer messageType);

    /**
     * 查询会话的所有消息
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0 ORDER BY created_at ASC")
    List<ChatMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 查询会话的消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0")
    Integer countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 查询会话的最后一条消息
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0 ORDER BY created_at DESC LIMIT 1")
    ChatMessage selectLastMessage(@Param("conversationId") Long conversationId);

    /**
     * 查询会话的用户消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND message_type = 1 AND is_deleted = 0 ORDER BY created_at ASC")
    List<ChatMessage> selectUserMessages(@Param("conversationId") Long conversationId);

    /**
     * 查询会话的AI回复列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND message_type = 2 AND is_deleted = 0 ORDER BY created_at ASC")
    List<ChatMessage> selectAiMessages(@Param("conversationId") Long conversationId);

    /**
     * 更新消息编辑状态
     */
    @Update("UPDATE chat_message SET content = #{content}, original_content = #{originalContent}, " +
            "edit_status = #{editStatus}, edit_count = edit_count + 1, edited_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateEditStatus(@Param("id") Long id,
                         @Param("content") String content,
                         @Param("originalContent") String originalContent,
                         @Param("editStatus") Integer editStatus);

    /**
     * 更新消息重新生成信息
     */
    @Update("UPDATE chat_message SET regenerate_count = regenerate_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementRegenerateCount(@Param("id") Long id);

    /**
     * 更新消息状态
     */
    @Update("UPDATE chat_message SET status = #{status}, error_msg = #{errorMsg}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("errorMsg") String errorMsg);

    /**
     * 更新Token使用量
     */
    @Update("UPDATE chat_message SET input_tokens = #{inputTokens}, output_tokens = #{outputTokens}, " +
            "total_tokens = #{totalTokens}, cost = #{cost}, updated_at = NOW() WHERE id = #{id}")
    int updateTokenUsage(@Param("id") Long id,
                         @Param("inputTokens") Integer inputTokens,
                         @Param("outputTokens") Integer outputTokens,
                         @Param("totalTokens") Integer totalTokens,
                         @Param("cost") BigDecimal cost);

    /**
     * 更新生成时间和延迟
     */
    @Update("UPDATE chat_message SET generate_time = #{generateTime}, first_token_latency = #{firstTokenLatency}, updated_at = NOW() WHERE id = #{id}")
    int updateGenerateStats(@Param("id") Long id,
                           @Param("generateTime") Long generateTime,
                           @Param("firstTokenLatency") Long firstTokenLatency);

    /**
     * 软删除消息
     */
    @Update("UPDATE chat_message SET is_deleted = 1, updated_at = NOW() WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    /**
     * 批量查询消息
     */
    List<ChatMessage> selectBatchIdsWithDeleted(@Param("ids") List<Long> ids);

    /**
     * 查询用户的所有消息
     */
    @Select("SELECT * FROM chat_message WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<ChatMessage> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE user_id = #{userId} AND is_deleted = 0")
    Integer countByUserId(@Param("userId") Long userId);
    
    /**
     * 软删除消息
     */
    @Update("UPDATE chat_message SET is_deleted = 1, updated_at = NOW() WHERE id = #{messageId}")
    int softDelete(@Param("messageId") Long messageId);

    /**
     * 更新消息编辑状态
     */
    @Update("UPDATE chat_message SET content = #{content}, original_content = #{originalContent}, edit_status = #{editStatus}, edit_count = edit_count + 1, edited_at = NOW(), updated_at = NOW() WHERE id = #{messageId}")
    int updateEditStatus(@Param("messageId") Long messageId,
                        @Param("content") String content,
                        @Param("originalContent") String originalContent,
                        @Param("editStatus") Integer editStatus);

    /**
     * 更新重新生成次数
     */
    @Update("UPDATE chat_message SET regenerate_count = regenerate_count + 1, updated_at = NOW() WHERE id = #{messageId}")
    int incrementRegenerateCount(@Param("messageId") Long messageId);

    /**
     * 更新消息状态
     */
    @Update("UPDATE chat_message SET status = #{status}, error_msg = #{errorMsg}, updated_at = NOW() WHERE id = #{messageId}")
    int updateStatus(@Param("messageId") Long messageId,
                    @Param("status") Integer status,
                    @Param("errorMsg") String errorMsg);

    /**
     * 更新AI消息内容（流式输出时使用）
     */
    @Update("UPDATE chat_message SET content = #{content}, updated_at = NOW() WHERE id = #{messageId}")
    int updateContent(@Param("messageId") Long messageId, @Param("content") String content);

    /**
     * 更新消息的Token使用统计
     */
    @Update("UPDATE chat_message SET input_tokens = #{inputTokens}, output_tokens = #{outputTokens}, total_tokens = #{totalTokens}, cost = #{cost}, updated_at = NOW() WHERE id = #{messageId}")
    int updateTokenUsage(@Param("messageId") Long messageId,
                        @Param("inputTokens") Integer inputTokens,
                        @Param("outputTokens") Integer outputTokens,
                        @Param("totalTokens") Integer totalTokens,
                        @Param("cost") java.math.BigDecimal cost);

    /**
     * 更新生成耗时
     */
    @Update("UPDATE chat_message SET generate_time = #{generateTime}, first_token_latency = #{firstTokenLatency}, updated_at = NOW() WHERE id = #{messageId}")
    int updateTiming(@Param("messageId") Long messageId,
                    @Param("generateTime") Long generateTime,
                    @Param("firstTokenLatency") Long firstTokenLatency);

    /**
     * 批量软删除消息
     */
    @Update("<script>" +
            "UPDATE chat_message SET is_deleted = 1, updated_at = NOW() WHERE id IN " +
            "<foreach collection='messageIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchSoftDelete(@Param("messageIds") List<Long> messageIds);

    /**
     * 获取会话的消息数量
     */
    @Update("SELECT COUNT(*) FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0")
    int countByConversationId(@Param("conversationId") Long conversationId);
}