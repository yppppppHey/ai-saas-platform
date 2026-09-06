package com.aisaas.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话创建DTO
 */
@Data
public class ConversationCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话标题
     */
    @NotBlank(message = "会话标题不能为空")
    private String title;

    /**
     * 使用的模型
     */
    @NotBlank(message = "模型不能为空")
    private String model;

    /**
     * AI Provider: openai/deepseek
     */
    private String provider;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 扩展字段(JSON)
     */
    private String extras;
}
