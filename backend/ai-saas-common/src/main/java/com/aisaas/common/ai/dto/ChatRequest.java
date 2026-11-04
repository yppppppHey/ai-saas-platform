package com.aisaas.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI 聊天请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 停止词
     */
    private List<String> stop;

    /**
     * Top P 采样
     */
    private Double topP;

    /**
     * 频率惩罚
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚
     */
    private Double presencePenalty;

    /**
     * 用户标识（用于追踪和防止滥用）
     */
    private String user;

    /**
     * 工具列表（函数调用）
     */
    private List<Tool> tools;

    /**
     * 工具选择策略
     */
    private Object toolChoice;

    /**
     * 响应格式
     */
    private ResponseFormat responseFormat;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 消息对象
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：system/user/assistant/tool
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 工具调用ID（用于tool角色的消息）
         */
        private String toolCallId;

        /**
         * 工具调用列表（用于assistant角色的消息）
         */
        private List<ToolCall> toolCalls;

        /**
         * 名称（用于区分不同用户）
         */
        private String name;
    }

    /**
     * 工具调用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        /**
         * 调用ID
         */
        private String id;

        /**
         * 调用类型
         */
        private String type;

        /**
         * 函数调用
         */
        private FunctionCall function;
    }

    /**
     * 函数调用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCall {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数（JSON字符串）
         */
        private String arguments;
    }

    /**
     * 工具定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tool {
        /**
         * 工具类型
         */
        private String type;

        /**
         * 函数定义
         */
        private Function function;
    }

    /**
     * 函数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数描述
         */
        private String description;

        /**
         * 参数定义（JSON Schema）
         */
        private Object parameters;
    }

    /**
     * 响应格式
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        /**
         * 格式类型：text/json_object
         */
        private String type;

        /**
         * JSON Schema（当type为json_object时）
         */
        private Object jsonSchema;
    }
}
