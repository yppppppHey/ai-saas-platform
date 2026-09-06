package com.aisaas.chat.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表项DTO
 */
@Data
public class ConversationListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * AI Provider
     */
    private String provider;

    /**
     * 置顶状态: 0-未置顶 1-已置顶
     */
    private Integer isPinned;

    /**
     * 归档状态: 0-未归档 1-已归档
     */
    private Integer isArchived;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * Token使用量
     */
    private Long tokenUsage;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageAt;

    /**
     * 最后消息内容预览
     */
    private String lastMessagePreview;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
