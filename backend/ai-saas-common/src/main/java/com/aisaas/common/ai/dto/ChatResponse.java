package com.aisaas.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 聊天响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 响应ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型ID
     */
    private String model;

    /**
     * 选择列表
     */
    private List<Choice> choices;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 系统指纹
     */
    private String systemFingerprint;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 原始响应（调试用）
     */
    private String rawResponse;

    /**
     * 错误信息
     */
    private ErrorInfo error;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return error == null && choices != null && !choices.isEmpty();
    }

    /**
     * 获取消息内容
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }

    /**
     * 获取首个返回消息的角色
     */
    public String getRole() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getRole();
        }
        return null;
    }

    /**
     * 获取工具调用
     */
    public List<ChatRequest.ToolCall> getToolCalls() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getToolCalls();
        }
        return null;
    }

    /**
     * 选择对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 消息
         */
        private Message message;

        /**
         * 流式输出时的增量消息（SSE 场景）
         */
        private Message delta;

        /**
         * 完成原因
         */
        private String finishReason;

        /**
         * 日志概率
         */
        private Object logprobs;
    }

    /**
     * 消息对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容
         */
        private String content;

        /**
         * 工具调用
         */
        private List<ChatRequest.ToolCall> toolCalls;

        /**
         * 工具调用ID
         */
        private String toolCallId;

        /**
         * 名称
         */
        private String name;
    }

    /**
     * Token使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 提示token数
         */
        private Integer promptTokens;

        /**
         * 完成token数
         */
        private Integer completionTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }

    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;

        /**
         * 错误类型
         */
        private String type;
    }
}
