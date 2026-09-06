package com.aisaas.chat.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiChatResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息类型: delta(流式片段)/complete(完整消息)/error(错误)/status(状态)
     */
    private String type;

    /**
     * 响应内容（流式时为增量内容）
     */
    private String content;

    /**
     * 完整内容（仅在type=complete时有效）
     */
    private String fullContent;

    /**
     * 是否完成
     */
    private Boolean done;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 输入Token数
     */
    private Integer inputTokens;

    /**
     * 输出Token数
     */
    private Integer outputTokens;

    /**
     * 总Token数
     */
    private Integer totalTokens;

    /**
     * 费用消耗
     */
    private BigDecimal cost;

    /**
     * 首字延迟（毫秒）
     */
    private Long firstTokenLatency;

    /**
     * 总生成时间（毫秒）
     */
    private Long totalTime;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 引用文档列表（RAG时使用）
     */
    private List<Citation> citations;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 引用文档信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 文档ID
         */
        private Long documentId;

        /**
         * 文档标题
         */
        private String title;

        /**
         * 引用内容
         */
        private String content;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 页码/位置
         */
        private String position;
    }

    /**
     * 生成状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 消息ID
         */
        private Long messageId;

        /**
         * 状态: generating(生成中)/completed(完成)/failed(失败)/stopped(停止)
         */
        private String status;

        /**
         * 已生成内容
         */
        private String generatedContent;

        /**
         * 生成进度(0-100)
         */
        private Integer progress;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 预计剩余时间(秒)
         */
        private Integer estimatedRemainingTime;
    }

    // 快速构建方法
    public static AiChatResponseDTO delta(Long messageId, String content) {
        return AiChatResponseDTO.builder()
                .messageId(messageId)
                .type("delta")
                .content(content)
                .done(false)
                .build();
    }

    public static AiChatResponseDTO complete(Long messageId, String fullContent) {
        return AiChatResponseDTO.builder()
                .messageId(messageId)
                .type("complete")
                .fullContent(fullContent)
                .done(true)
                .build();
    }

    public static AiChatResponseDTO error(Long messageId, String errorCode, String error) {
        return AiChatResponseDTO.builder()
                .messageId(messageId)
                .type("error")
                .errorCode(errorCode)
                .error(error)
                .done(true)
                .build();
    }
}