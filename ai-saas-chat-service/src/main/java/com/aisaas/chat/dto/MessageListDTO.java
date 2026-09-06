package com.aisaas.chat.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消息列表项DTO
 */
@Data
public class MessageListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息类型: 1-用户消息 2-AI回复 3-系统消息
     */
    private Integer messageType;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 父消息ID
     */
    private Long parentId;

    /**
     * 回复的消息ID
     */
    private Long replyToId;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 总Token数
     */
    private Integer totalTokens;

    /**
     * 费用消耗
     */
    private BigDecimal cost;

    /**
     * 编辑状态: 0-未编辑 1-已编辑 2-已删除
     */
    private Integer editStatus;

    /**
     * 编辑时间
     */
    private LocalDateTime editedAt;

    /**
     * 重新生成次数
     */
    private Integer regenerateCount;

    /**
     * 状态: 0-生成中 1-完成 2-失败 3-取消
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 附件列表
     */
    private String attachments;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
