package com.aisaas.common.ai.agent.node;

import com.aisaas.common.ai.agent.workflow.WorkflowNode;

/**
 * 节点执行器接口
 * 定义执行工作流节点的标准接口
 */
public interface NodeExecutor {

    /**
     * 获取执行器支持的节点类型
     *
     * @return 节点类型，如 "LLM", "TOOL", "CONDITION" 等
     */
    String getNodeType();

    /**
     * 执行节点
     *
     * @param node    要执行的节点定义
     * @param context 执行上下文
     * @return 执行结果
     */
    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);

    /**
     * 验证节点配置是否有效
     *
     * @param node 要验证的节点
     * @return 验证结果
     */
    default boolean validate(WorkflowNode node) {
        return true;
    }

    /**
     * 是否需要异步执行
     *
     * @param node 节点定义
     * @return 是否异步执行
     */
    default boolean isAsync(WorkflowNode node) {
        return false;
    }

    /**
     * 获取执行超时时间（秒）
     *
     * @param node 节点定义
     * @return 超时时间，0表示不限制
     */
    default int getTimeoutSeconds(WorkflowNode node) {
        int timeout = node.getTimeoutSeconds();
        return timeout > 0 ? timeout : 60; // 默认60秒
    }

    /**
     * 是否可以重试
     *
     * @param node 节点定义
     * @return 是否支持重试
     */
    default boolean isRetryable(WorkflowNode node) {
        return node.getMaxRetries() > 0;
    }

    /**
     * 获取最大重试次数
     *
     * @param node 节点定义
     * @return 最大重试次数
     */
    default int getMaxRetries(WorkflowNode node) {
        return node.getMaxRetries();
    }
}
