package com.aisaas.common.ai.agent.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流实例
 * 表示一个正在运行或已完成的工作流执行实例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String id;

    /**
     * 工作流定义ID
     */
    private String workflowDefinitionId;

    /**
     * 工作流版本
     */
    private String workflowVersion;

    /**
     * 实例状态
     * PENDING: 等待中
     * RUNNING: 运行中
     * PAUSED: 已暂停
     * COMPLETED: 已完成
     * FAILED: 失败
     * CANCELLED: 已取消
     * TIMEOUT: 超时
     * WAITING_FOR_INPUT: 等待用户输入
     */
    @Builder.Default
    private String status = "PENDING";

    /**
     * 当前执行节点ID
     */
    private String currentNodeId;

    /**
     * 已执行过的节点列表
     */
    @Builder.Default
    private Map<String, NodeExecutionRecord> executedNodes = new ConcurrentHashMap<>();

    /**
     * 全局变量存储
     */
    @Builder.Default
    private Map<String, Object> variables = new ConcurrentHashMap<>();

    /**
     * 输入参数
     */
    @Builder.Default
    private Map<String, Object> inputs = new HashMap<>();

    /**
     * 输出结果
     */
    @Builder.Default
    private Map<String, Object> outputs = new HashMap<>();

    /**
     * 执行上下文
     */
    @Builder.Default
    private Map<String, Object> context = new ConcurrentHashMap<>();

    /**
     * 父实例ID（子工作流时使用）
     */
    private String parentInstanceId;

    /**
     * 触发来源
     */
    private String triggerSource;

    /**
     * 触发用户ID
     */
    private String triggeredBy;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

    // ============ 实例状态常量 ============

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
    public static final String STATUS_WAITING_FOR_INPUT = "WAITING_FOR_INPUT";

    // ============ 业务方法 ============

    /**
     * 检查实例是否处于活跃状态
     */
    public boolean isActive() {
        return STATUS_PENDING.equals(status)
                || STATUS_RUNNING.equals(status)
                || STATUS_PAUSED.equals(status)
                || STATUS_WAITING_FOR_INPUT.equals(status);
    }

    /**
     * 检查实例是否已完成
     */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_CANCELLED.equals(status)
                || STATUS_TIMEOUT.equals(status);
    }

    /**
     * 检查实例是否失败
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    /**
     * 获取变量（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, T defaultValue) {
        Object value = variables.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 添加上下文数据
     */
    public void addContext(String key, Object value) {
        context.put(key, value);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) context.get(key);
    }

    /**
     * 记录节点执行
     */
    public void recordNodeExecution(String nodeId, NodeExecutionRecord record) {
        executedNodes.put(nodeId, record);
        currentNodeId = nodeId;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取节点执行记录
     */
    public NodeExecutionRecord getNodeExecution(String nodeId) {
        return executedNodes.get(nodeId);
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage, String errorStack) {
        this.status = STATUS_FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为完成
     */
    public void markAsCompleted(Map<String, Object> outputs) {
        this.status = STATUS_COMPLETED;
        if (outputs != null) {
            this.outputs.putAll(outputs);
        }
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新状态
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        if (STATUS_RUNNING.equals(newStatus) && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * 节点执行记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeExecutionRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeId;
        private String nodeName;
        private String nodeType;

        // 执行状态
        private String status; // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED

        // 执行时间
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // 执行时长（毫秒）
        private Long durationMs;

        // 输入数据
        private Map<String, Object> inputs;

        // 输出数据
        private Map<String, Object> outputs;

        // 错误信息
        private String errorMessage;
        private String errorStack;

        // 重试次数
        private int retryCount;

        // 额外元数据
        private Map<String, Object> metadata;

        // 子实例ID（用于子工作流节点）
        private String subInstanceId;

        public void markAsCompleted(Map<String, Object> outputs) {
            this.status = "COMPLETED";
            this.endTime = LocalDateTime.now();
            if (this.startTime != null) {
                this.durationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
            }
            if (outputs != null) {
                this.outputs = outputs;
            }
        }

        public void markAsFailed(String errorMessage, String errorStack) {
            this.status = "FAILED";
            this.endTime = LocalDateTime.now();
            if (this.startTime != null) {
                this.durationMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
            }
            this.errorMessage = errorMessage;
            this.errorStack = errorStack;
        }
    }
}
