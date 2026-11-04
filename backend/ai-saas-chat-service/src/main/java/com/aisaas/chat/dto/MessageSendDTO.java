package com.aisaas.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息发送DTO
 */
@Data
public class MessageSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 内容类型: text/image/file/audio
     */
    private String contentType = "text";

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 引用的消息ID
     */
    private Long replyToId;

    /**
     * 附件列表(JSON)
     */
    private String attachments;

    /**
     * 是否使用流式响应
     */
    private Boolean stream = true;

    /**
     * 扩展字段
     */
    private String extras;
}
