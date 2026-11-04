package com.aisaas.chat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会话统计DTO
 */
@Data
public class ConversationStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总会话数
     */
    private Integer totalConversations;

    /**
     * 置顶会话数
     */
    private Integer pinnedConversations;

    /**
     * 归档会话数
     */
    private Integer archivedConversations;

    /**
     * 总消息数
     */
    private Integer totalMessages;

    /**
     * 总Token使用量
     */
    private Long totalTokens;
}
