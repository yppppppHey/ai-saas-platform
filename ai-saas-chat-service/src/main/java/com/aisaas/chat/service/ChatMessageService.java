package com.aisaas.chat.service;

import com.aisaas.chat.dto.*;
import com.aisaas.chat.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 消息服务接口
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 发送文本消息
     *
     * @param userId 用户ID
     * @param dto    发送DTO
     * @return 发送的消息
     */
    ChatMessage sendTextMessage(Long userId, MessageSendDTO dto);

    /**
     * 发送文件消息
     *
     * @param userId 用户ID
     * @param dto    发送DTO
     * @return 发送的消息
     */
    ChatMessage sendFileMessage(Long userId, MessageSendDTO dto);

    /**
     * 编辑消息
     *
     * @param userId  用户ID
     * @param messageId 消息ID
     * @param dto     编辑DTO
     * @return 编辑后的消息
     */
    ChatMessage editMessage(Long userId, Long messageId, MessageEditDTO dto);

    /**
     * 删除消息
     *
     * @param userId    用户ID
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean deleteMessage(Long userId, Long messageId);

    /**
     * 批量删除消息
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return 是否成功
     */
    boolean batchDeleteMessages(Long userId, List<Long> messageIds);

    /**
     * 重新生成AI回复
     *
     * @param userId 用户ID
     * @param dto    重新生成DTO
     * @return 新生成的消息
     */
    ChatMessage regenerateMessage(Long userId, MessageRegenerateDTO dto);

    /**
     * 分页查询会话消息历史
     *
     * @param conversationId 会话ID
     * @param messageType    消息类型
     * @param pageNum        页码
     * @param pageSize       每页大小
     * @return 分页结果
     */
    Page<MessageListDTO> listMessages(Long conversationId, Integer messageType, Integer pageNum, Integer pageSize);

    /**
     * 查询会话的所有消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<MessageListDTO> getConversationMessages(Long conversationId);

    /**
     * 获取消息详情
     *
     * @param messageId 消息ID
     * @return 消息详情
     */
    MessageListDTO getMessageDetail(Long messageId);

    /**
     * 查询会话的消息数量
     *
     * @param conversationId 会话ID
     * @return 消息数量
     */
    Integer getMessageCount(Long conversationId);

    /**
     * 查询用户的所有消息
     *
     * @param userId 用户ID
     * @return 消息列表
     */
    List<ChatMessage> getUserMessages(Long userId);

    /**
     * 更新消息Token使用量
     *
     * @param messageId    消息ID
     * @param inputTokens  输入Token数
     * @param outputTokens 输出Token数
     * @param cost         费用
     * @return 是否成功
     */
    boolean updateMessageTokenUsage(Long messageId, Integer inputTokens, Integer outputTokens, java.math.BigDecimal cost);

    /**
     * 更新消息生成统计
     *
     * @param messageId         消息ID
     * @param generateTime      生成耗时
     * @param firstTokenLatency 首字延迟
     * @return 是否成功
     */
    boolean updateMessageGenerateStats(Long messageId, Long generateTime, Long firstTokenLatency);

    /**
     * 更新消息状态
     *
     * @param messageId 消息ID
     * @param status    状态
     * @param errorMsg  错误信息
     * @return 是否成功
     */
    boolean updateMessageStatus(Long messageId, Integer status, String errorMsg);
}
