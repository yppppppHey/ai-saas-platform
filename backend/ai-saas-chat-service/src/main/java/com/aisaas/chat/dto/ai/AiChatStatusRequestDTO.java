package com.aisaas.chat.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * AI对话状态查询请求DTO
 */
@Data
public class AiChatStatusRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;
}