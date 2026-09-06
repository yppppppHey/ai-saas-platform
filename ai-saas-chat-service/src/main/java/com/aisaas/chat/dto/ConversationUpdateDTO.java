package com.aisaas.chat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会话更新DTO
 */
@Data
public class ConversationUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 置顶状态: 0-未置顶 1-已置顶
     */
    private Integer isPinned;

    /**
     * 归档状态: 0-未归档 1-已归档
     */
    private Integer isArchived;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 扩展字段(JSON)
     */
    private String extras;
}
