package com.aisaas.common.ai.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 定义
 * 定义可被Agent调用的工具接口
 */
public interface Tool {

    /**
     * 获取工具名称
     * 唯一标识符
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 获取工具参数定义
     */
    List<Parameter> getParameters();

    /**
     * 执行工具
     *
     * @param params 工具参数
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * 验证参数是否有效
     *
     * @param params 待验证的参数
     * @return 验证结果
     */
    default ValidationResult validate(Map<String, Object> params) {
        ValidationResult result = new ValidationResult();

        for (Parameter param : getParameters()) {
            Object value = params.get(param.getName());

            // 检查必填参数
            if (param.isRequired() && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
                result.addError(param.getName(), "Parameter is required");
                continue;
            }

            // 如果有值，验证类型
            if (value != null) {
                if (!isValidType(value, param.getType())) {
                    result.addError(param.getName(), "Invalid type, expected: " + param.getType());
                }
            }
        }

        return result;
    }

    /**
     * 检查值是否符合参数类型
     */
    private boolean isValidType(Object value, String expectedType) {
        if (value == null) {
            return true;
        }

        switch (expectedType.toLowerCase()) {
            case "string":
                return value instanceof String;
            case "integer":
            case "int":
                return value instanceof Integer || value instanceof Long;
            case "number":
            case "float":
            case "double":
                return value instanceof Number;
            case "boolean":
            case "bool":
                return value instanceof Boolean;
            case "array":
            case "list":
                return value instanceof java.util.List || value.getClass().isArray();
            case "object":
            case "map":
                return value instanceof java.util.Map;
            default:
                return true;
        }
    }

    // ============ 内部类定义 ============

    /**
     * 工具参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 参数类型
         */
        private String type;

        /**
         * 是否必填
         */
        @Builder.Default
        private boolean required = false;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 枚举值（如果有）
         */
        private java.util.List<String> enumValues;
    }

    /**
     * 工具执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        /**
         * 是否成功
         */
        @Builder.Default
        private boolean success = true;

        /**
         * 输出数据
         */
        @Builder.Default
        private Map<String, Object> data = new HashMap<>();

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 执行时间（毫秒）
         */
        private Long executionTimeMs;

        /**
         * 创建成功结果
         */
        public static ToolResult success() {
            return ToolResult.builder().success(true).build();
        }

        /**
         * 创建成功结果（带数据）
         */
        public static ToolResult success(Map<String, Object> data) {
            return ToolResult.builder()
                    .success(true)
                    .data(data != null ? data : new HashMap<>())
                    .build();
        }

        /**
         * 创建成功结果（带单个值）
         */
        public static ToolResult success(String key, Object value) {
            Map<String, Object> data = new HashMap<>();
            data.put(key, value);
            return ToolResult.builder().success(true).data(data).build();
        }

        /**
         * 创建失败结果
         */
        public static ToolResult failure(String errorMessage) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    /**
     * 验证结果
     */
    @Data
    @NoArgsConstructor
    public static class ValidationResult {
        private final java.util.List<ValidationError> errors = new java.util.ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public void addError(String field, String message) {
            errors.add(new ValidationError(field, message));
        }

        @Data
        @AllArgsConstructor
        public static class ValidationError {
            private String field;
            private String message;
        }
    }
}
