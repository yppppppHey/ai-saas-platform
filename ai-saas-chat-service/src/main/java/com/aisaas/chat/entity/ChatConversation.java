package com.aisaas.chat.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_conversation")
public class ChatConversation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField("user_id")
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
     * AI Provider: openai/deepseek
     */
    private String provider;

    /**
     * 系统提示词
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 置顶状态: 0-未置顶 1-已置顶
     */
    @TableField("is_pinned")
    private Integer isPinned;

    /**
     * 归档状态: 0-未归档 1-已归档
     */
    @TableField("is_archived")
    private Integer isArchived;

    /**
     * 消息数量
     */
    @TableField("message_count")
    private Integer messageCount;

    /**
     * Token使用量
     */
    @TableField("token_usage")
    private Long tokenUsage;

    /**
     * 最后消息时间
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 最后消息内容预览
     */
    @TableField("last_message_preview")
    private String lastMessagePreview;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 扩展字段(JSON)
     */
    private String extras;
}
