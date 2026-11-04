package com.aisaas.common.ai.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流边（连接）
 * 定义工作流节点之间的连接关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 边ID
     */
    private String id;

    /**
     * 源节点ID
     */
    private String sourceNodeId;

    /**
     * 目标节点ID
     */
    private String targetNodeId;

    /**
     * 边的类型
     * DEFAULT: 默认连接
     * TRUE: 条件为真时的连接
     * FALSE: 条件为假时的连接
     * LOOP: 循环连接
     * ERROR: 错误处理连接
     * CUSTOM: 自定义类型
     */
    @Builder.Default
    private String type = "DEFAULT";

    /**
     * 条件表达式（用于条件分支）
     */
    private String condition;

    /**
     * 边的描述
     */
    private String description;

    /**
     * 边的权重（用于路径优化）
     */
    @Builder.Default
    private int weight = 1;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * 数据映射规则
     * 定义源节点输出如何映射到目标节点输入
     */
    @Builder.Default
    private Map<String, String> dataMapping = new HashMap<>();

    // ============ 边类型常量 ============

    public static final String TYPE_DEFAULT = "DEFAULT";
    public static final String TYPE_TRUE = "TRUE";
    public static final String TYPE_FALSE = "FALSE";
    public static final String TYPE_LOOP = "LOOP";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_CUSTOM = "CUSTOM";

    /**
     * 是否为条件边
     */
    public boolean isConditional() {
        return TYPE_TRUE.equals(type) || TYPE_FALSE.equals(type)
                || (condition != null && !condition.isEmpty());
    }

    /**
     * 是否为错误处理边
     */
    public boolean isErrorEdge() {
        return TYPE_ERROR.equals(type);
    }

    /**
     * 是否为循环边
     */
    public boolean isLoopEdge() {
        return TYPE_LOOP.equals(type);
    }

    /**
     * 创建默认边
     */
    public static WorkflowEdge createDefaultEdge(String id, String sourceNodeId, String targetNodeId) {
        return WorkflowEdge.builder()
                .id(id)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .type(TYPE_DEFAULT)
                .build();
    }

    /**
     * 创建条件边（真分支）
     */
    public static WorkflowEdge createTrueEdge(String id, String sourceNodeId, String targetNodeId) {
        return WorkflowEdge.builder()
                .id(id)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .type(TYPE_TRUE)
                .build();
    }

    /**
     * 创建条件边（假分支）
     */
    public static WorkflowEdge createFalseEdge(String id, String sourceNodeId, String targetNodeId) {
        return WorkflowEdge.builder()
                .id(id)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .type(TYPE_FALSE)
                .build();
    }

    /**
     * 创建循环边
     */
    public static WorkflowEdge createLoopEdge(String id, String sourceNodeId, String targetNodeId, String condition) {
        return WorkflowEdge.builder()
                .id(id)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .type(TYPE_LOOP)
                .condition(condition)
                .build();
    }

    /**
     * 创建错误处理边
     */
    public static WorkflowEdge createErrorEdge(String id, String sourceNodeId, String targetNodeId) {
        return WorkflowEdge.builder()
                .id(id)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .type(TYPE_ERROR)
                .build();
    }
}
