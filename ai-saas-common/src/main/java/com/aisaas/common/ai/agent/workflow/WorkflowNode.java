package com.aisaas.common.ai.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流节点
 * 定义工作流中的一个节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点类型
     * START: 开始节点
     * END: 结束节点
     * LLM: LLM调用节点
     * TOOL: 工具调用节点
     * CONDITION: 条件分支节点
     * LOOP: 循环节点
     * SUBFLOW: 子工作流节点
     * PARALLEL: 并行节点
     * AGGREGATE: 聚合节点
     * DELAY: 延迟节点
     * SET_VARIABLE: 变量设置节点
     * CUSTOM: 自定义节点
     */
    private String type;

    /**
     * 节点位置（用于可视化）
     */
    private Position position;

    /**
     * 节点配置参数
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * 输入参数映射
     * key: 节点内部参数名
     * value: 参数来源（如 ${variableName} 或 ${nodeId.outputName}）
     */
    @Builder.Default
    private Map<String, String> inputs = new HashMap<>();

    /**
     * 输出参数映射
     * key: 节点内部输出名
     * value: 输出别名
     */
    @Builder.Default
    private Map<String, String> outputs = new HashMap<>();

    /**
     * 是否为开始节点
     */
    @Builder.Default
    private boolean startNode = false;

    /**
     * 是否为结束节点
     */
    @Builder.Default
    private boolean endNode = false;

    /**
     * 超时时间（秒），0表示不限制
     */
    @Builder.Default
    private int timeoutSeconds = 0;

    /**
     * 重试次数
     */
    @Builder.Default
    private int maxRetries = 0;

    /**
     * 重试间隔（秒）
     */
    @Builder.Default
    private int retryIntervalSeconds = 1;

    /**
     * 失败处理策略
     * CONTINUE: 继续执行后续节点
     * STOP: 停止工作流
     * SKIP: 跳过当前节点
     * GOTO: 跳转到指定节点
     */
    @Builder.Default
    private String onFailure = "STOP";

    /**
     * 失败时跳转的节点ID（当onFailure为GOTO时使用）
     */
    private String onFailureGotoNodeId;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 位置信息（用于可视化）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }

    // ============ 节点类型常量 ============

    public static final String TYPE_START = "START";
    public static final String TYPE_END = "END";
    public static final String TYPE_LLM = "LLM";
    public static final String TYPE_TOOL = "TOOL";
    public static final String TYPE_CONDITION = "CONDITION";
    public static final String TYPE_LOOP = "LOOP";
    public static final String TYPE_SUBFLOW = "SUBFLOW";
    public static final String TYPE_PARALLEL = "PARALLEL";
    public static final String TYPE_AGGREGATE = "AGGREGATE";
    public static final String TYPE_DELAY = "DELAY";
    public static final String TYPE_SET_VARIABLE = "SET_VARIABLE";
    public static final String TYPE_CUSTOM = "CUSTOM";

    // ============ 失败处理策略常量 ============

    public static final String ON_FAILURE_CONTINUE = "CONTINUE";
    public static final String ON_FAILURE_STOP = "STOP";
    public static final String ON_FAILURE_SKIP = "SKIP";
    public static final String ON_FAILURE_GOTO = "GOTO";

    /**
     * 判断是否为控制流节点
     */
    public boolean isControlNode() {
        return TYPE_START.equals(type)
                || TYPE_END.equals(type)
                || TYPE_CONDITION.equals(type)
                || TYPE_LOOP.equals(type)
                || TYPE_PARALLEL.equals(type)
                || TYPE_AGGREGATE.equals(type);
    }

    /**
     * 判断是否为执行节点
     */
    public boolean isExecuteNode() {
        return TYPE_LLM.equals(type)
                || TYPE_TOOL.equals(type)
                || TYPE_SUBFLOW.equals(type)
                || TYPE_CUSTOM.equals(type);
    }

    /**
     * 判断是否需要等待外部输入
     */
    public boolean requiresExternalInput() {
        return TYPE_START.equals(type) && config.containsKey("waitForInput")
                && Boolean.TRUE.equals(config.get("waitForInput"));
    }

    /**
     * 获取配置项
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, T defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * 设置配置项
     */
    public void setConfig(String key, Object value) {
        config.put(key, value);
    }

    /**
     * 创建开始节点
     */
    public static WorkflowNode createStartNode(String id, String name) {
        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_START)
                .startNode(true)
                .build();
    }

    /**
     * 创建结束节点
     */
    public static WorkflowNode createEndNode(String id, String name) {
        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_END)
                .endNode(true)
                .build();
    }

    /**
     * 创建LLM节点
     */
    public static WorkflowNode createLlmNode(String id, String name, String prompt,
                                              String model, String provider) {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", prompt);
        config.put("model", model);
        config.put("provider", provider);

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_LLM)
                .config(config)
                .build();
    }

    /**
     * 创建工具调用节点
     */
    public static WorkflowNode createToolNode(String id, String name, String toolName,
                                               Map<String, Object> toolParams) {
        Map<String, Object> config = new HashMap<>();
        config.put("toolName", toolName);
        config.put("toolParams", toolParams);

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_TOOL)
                .config(config)
                .build();
    }

    /**
     * 创建条件分支节点
     */
    public static WorkflowNode createConditionNode(String id, String name,
                                                    List<ConditionBranch> branches) {
        Map<String, Object> config = new HashMap<>();
        config.put("branches", branches);

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_CONDITION)
                .config(config)
                .build();
    }

    /**
     * 创建循环节点
     */
    public static WorkflowNode createLoopNode(String id, String name, String condition,
                                               int maxIterations) {
        Map<String, Object> config = new HashMap<>();
        config.put("condition", condition);
        config.put("maxIterations", maxIterations);

        return WorkflowNode.builder()
                .id(id)
                .name(name)
                .type(TYPE_LOOP)
                .config(config)
                .build();
    }

    /**
     * 条件分支定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionBranch {
        private String id;
        private String name;
        private String condition;  // 条件表达式
        private String targetNodeId;  // 满足条件时跳转的节点
    }
}
