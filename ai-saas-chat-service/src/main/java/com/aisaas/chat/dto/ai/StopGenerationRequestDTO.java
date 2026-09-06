package com.aisaas.chat.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 停止生成请求DTO
 */
@Data
public class StopGenerationRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 正在生成的消息ID
     */
    @NotNull(message = "消息ID不能为空")
    private Long messageId;
}