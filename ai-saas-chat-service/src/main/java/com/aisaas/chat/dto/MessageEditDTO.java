package com.aisaas.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息编辑DTO
 */
@Data
public class MessageEditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 新的消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 是否重新生成AI回复
     */
    private Boolean regenerateReply = false;
}
