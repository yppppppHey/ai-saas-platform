package com.aisaas.chat.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 消息类型: 1-用户消息 2-AI回复 3-系统消息
     */
    @TableField("message_type")
    private Integer messageType;

    /**
     * 内容类型: text/image/file/audio
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 原始内容(用于编辑历史)
     */
    @TableField("original_content")
    private String originalContent;

    /**
     * 父消息ID(用于回复/引用)
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 回复的消息ID
     */
    @TableField("reply_to_id")
    private Long replyToId;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 模型版本
     */
    @TableField("model_version")
    private String modelVersion;

    /**
     * 输入Token数
     */
    @TableField("input_tokens")
    private Integer inputTokens;

    /**
     * 输出Token数
     */
    @TableField("output_tokens")
    private Integer outputTokens;

    /**
     * 总Token数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 费用消耗
     */
    private BigDecimal cost;

    /**
     * 生成耗时(毫秒)
     */
    @TableField("generate_time")
    private Long generateTime;

    /**
     * 首字延迟(毫秒)
     */
    @TableField("first_token_latency")
    private Long firstTokenLatency;

    /**
     * 编辑状态: 0-未编辑 1-已编辑 2-已删除
     */
    @TableField("edit_status")
    private Integer editStatus;

    /**
     * 编辑时间
     */
    @TableField("edited_at")
    private LocalDateTime editedAt;

    /**
     * 编辑次数
     */
    @TableField("edit_count")
    private Integer editCount;

    /**
     * 重新生成次数
     */
    @TableField("regenerate_count")
    private Integer regenerateCount;

    /**
     * 重新生成的源消息ID
     */
    @TableField("regenerate_from_id")
    private Long regenerateFromId;

    /**
     * 状态: 0-生成中 1-完成 2-失败 3-取消
     */
    private Integer status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 文件附件(JSON)
     */
    private String attachments;

    /**
     * 引用文档ID列表(JSON)
     */
    @TableField("citation_ids")
    private String citationIds;

    /**
     * 扩展字段(JSON)
     */
    private String extras;
}
