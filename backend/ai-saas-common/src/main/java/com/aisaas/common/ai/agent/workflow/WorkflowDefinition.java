package com.aisaas.common.ai.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 * 描述Agent工作流的完整结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private String id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 工作流版本
     */
    private String version;

    /**
     * 节点列表
     */
    @Builder.Default
    private List<WorkflowNode> nodes = new ArrayList<>();

    /**
     * 连接列表（边）
     */
    @Builder.Default
    private List<WorkflowEdge> edges = new ArrayList<>();

    /**
     * 全局变量定义
     */
    @Builder.Default
    private Map<String, VariableDef> variables = new HashMap<>();

    /**
     * 输入参数定义
     */
    @Builder.Default
    private Map<String, ParameterDef> inputParameters = new HashMap<>();

    /**
     * 输出定义
     */
    @Builder.Default
    private Map<String, OutputDef> outputs = new HashMap<>();

    /**
     * 起始节点ID
     */
    private String startNodeId;

    /**
     * 超时时间（秒）
     */
    @Builder.Default
    private int timeoutSeconds = 300;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * 重试间隔（秒）
     */
    @Builder.Default
    private int retryIntervalSeconds = 5;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 状态：DRAFT, PUBLISHED, DEPRECATED, ARCHIVED
     */
    @Builder.Default
    private String status = "DRAFT";

    // ============ 业务方法 ============

    /**
     * 根据ID查找节点
     */
    public WorkflowNode findNodeById(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找起始节点
     */
    public WorkflowNode findStartNode() {
        if (startNodeId != null) {
            return findNodeById(startNodeId);
        }
        return nodes.stream()
                .filter(WorkflowNode::isStartNode)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取节点的出边
     */
    public List<WorkflowEdge> getOutgoingEdges(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getSourceNodeId().equals(nodeId))
                .toList();
    }

    /**
     * 获取节点的入边
     */
    public List<WorkflowEdge> getIncomingEdges(String nodeId) {
        return edges.stream()
                .filter(edge -> edge.getTargetNodeId().equals(nodeId))
                .toList();
    }

    /**
     * 验证工作流定义
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // 检查基本属性
        if (id == null || id.isBlank()) {
            result.addError("id", "工作流ID不能为空");
        }
        if (name == null || name.isBlank()) {
            result.addError("name", "工作流名称不能为空");
        }

        // 检查节点
        if (nodes == null || nodes.isEmpty()) {
            result.addError("nodes", "工作流至少需要一个节点");
        } else {
            // 检查是否有且只有一个开始节点
            long startNodeCount = nodes.stream()
                    .filter(WorkflowNode::isStartNode)
                    .count();
            if (startNodeCount == 0) {
                result.addWarning("nodes", "没有标记开始节点，将使用第一个节点作为开始节点");
            } else if (startNodeCount > 1) {
                result.addError("nodes", "只能有一个开始节点");
            }

            // 检查节点ID唯一性
            long uniqueNodeIds = nodes.stream()
                    .map(WorkflowNode::getId)
                    .distinct()
                    .count();
            if (uniqueNodeIds != nodes.size()) {
                result.addError("nodes", "节点ID必须唯一");
            }
        }

        // 检查边
        if (edges != null) {
            for (WorkflowEdge edge : edges) {
                if (findNodeById(edge.getSourceNodeId()) == null) {
                    result.addError("edges", "边的源节点不存在: " + edge.getSourceNodeId());
                }
                if (findNodeById(edge.getTargetNodeId()) == null) {
                    result.addError("edges", "边的目标节点不存在: " + edge.getTargetNodeId());
                }
            }
        }

        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableDef {
        private String name;
        private String type;
        private String description;
        private Object defaultValue;
        private boolean required;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDef {
        private String name;
        private String type;
        private String description;
        private Object defaultValue;
        private boolean required;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputDef {
        private String name;
        private String type;
        private String description;
        private String mapping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        @Builder.Default
        private List<ValidationError> errors = new ArrayList<>();
        @Builder.Default
        private List<ValidationWarning> warnings = new ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public void addError(String field, String message) {
            errors.add(new ValidationError(field, message));
        }

        public void addWarning(String field, String message) {
            warnings.add(new ValidationWarning(field, message));
        }
    }

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class ValidationWarning {
        private String field;
        private String message;
    }
}
