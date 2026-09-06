package com.aisaas.common.ai.agent.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行结果
 * 封装节点执行的结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    /**
     * 是否执行成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 输出数据
     */
    @Builder.Default
    private Map<String, Object> outputs = new HashMap<>();

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 是否需要等待（异步执行时使用）
     */
    @Builder.Default
    private boolean waiting = false;

    /**
     * 等待原因
     */
    private String waitingReason;

    /**
     * 执行耗时（毫秒）
     */
    private Long executionTimeMs;

    /**
     * 执行的Token数量（用于LLM节点）
     */
    private Integer tokenCount;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // ============ 便捷方法 ============

    /**
     * 创建成功结果
     */
    public static NodeExecutionResult success() {
        return NodeExecutionResult.builder()
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（带输出）
     */
    public static NodeExecutionResult success(Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .success(true)
                .outputs(outputs != null ? outputs : new HashMap<>())
                .build();
    }

    /**
     * 创建成功结果（带单个输出）
     */
    public static NodeExecutionResult success(String key, Object value) {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put(key, value);
        return NodeExecutionResult.builder()
                .success(true)
                .outputs(outputs)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static NodeExecutionResult failure(String errorMessage) {
        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败结果（带堆栈）
     */
    public static NodeExecutionResult failure(String errorMessage, String errorStack) {
        return NodeExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorStack(errorStack)
                .build();
    }

    /**
     * 创建等待结果
     */
    public static NodeExecutionResult waiting(String reason) {
        return NodeExecutionResult.builder()
                .success(true)
                .waiting(true)
                .waitingReason(reason)
                .build();
    }

    /**
     * 添加输出
     */
    public NodeExecutionResult addOutput(String key, Object value) {
        if (outputs == null) {
            outputs = new HashMap<>();
        }
        outputs.put(key, value);
        return this;
    }

    /**
     * 批量添加输出
     */
    public NodeExecutionResult addOutputs(Map<String, Object> outputs) {
        if (this.outputs == null) {
            this.outputs = new HashMap<>();
        }
        if (outputs != null) {
            this.outputs.putAll(outputs);
        }
        return this;
    }

    /**
     * 添加元数据
     */
    public NodeExecutionResult addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
}
