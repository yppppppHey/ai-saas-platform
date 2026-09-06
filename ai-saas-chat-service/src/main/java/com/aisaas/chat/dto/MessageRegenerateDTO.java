package com.aisaas.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 消息重新生成DTO
 */
@Data
public class MessageRegenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * AI回复消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 是否使用相同的上下文
     */
    private Boolean useSameContext = true;

    /**
     * 指定的模型(可选，默认使用原模型)
     */
    private String model;
}
