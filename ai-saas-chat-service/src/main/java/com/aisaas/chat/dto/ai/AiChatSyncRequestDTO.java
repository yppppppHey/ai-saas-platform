package com.aisaas.chat.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * AI同步对话请求DTO（非流式）
 */
@Data
public class AiChatSyncRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 引用的消息ID
     */
    private Long replyToId;

    /**
     * 使用的模型（可选，默认使用会话配置的模型）
     */
    private String model;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature = 0.7;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 使用的提示词模板ID
     */
    private Long promptTemplateId;

    /**
     * 模板变量值
     */
    private Map<String, Object> templateVariables;

    /**
     * 是否使用RAG检索
     */
    private Boolean useRag = false;

    /**
     * 知识库ID列表（RAG使用）
     */
    private java.util.List<Long> knowledgeBaseIds;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
}