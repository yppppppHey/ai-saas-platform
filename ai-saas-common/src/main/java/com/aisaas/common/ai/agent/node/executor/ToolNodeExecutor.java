package com.aisaas.common.ai.agent.node.executor;

import com.aisaas.common.ai.agent.node.NodeExecutionContext;
import com.aisaas.common.ai.agent.node.NodeExecutionResult;
import com.aisaas.common.ai.agent.node.NodeExecutor;
import com.aisaas.common.ai.agent.tool.Tool;
import com.aisaas.common.ai.agent.tool.ToolRegistry;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具节点执行器
 * 调用已注册的工具执行特定功能
 */
@Slf4j
@Component
public class ToolNodeExecutor implements NodeExecutor {

    @Autowired
    private ToolRegistry toolRegistry;

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_TOOL;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing tool node: {}", node.getId());

        try {
            // 获取工具配置
            String toolName = node.getConfig("toolName", "").toString();
            if (toolName.isEmpty()) {
                return NodeExecutionResult.failure("Tool name not specified");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> toolParams = (Map<String, Object>) node.getConfig("toolParams", new HashMap<String, Object>());

            // 解析参数中的变量
            Map<String, Object> resolvedParams = resolveParameters(toolParams, context);

            // 查找工具
            Tool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                return NodeExecutionResult.failure("Tool not found: " + toolName);
            }

            // 执行工具
            log.debug("Calling tool: {}, params={}", toolName, resolvedParams);
            long startTime = System.currentTimeMillis();

            Tool.ToolResult result = tool.execute(resolvedParams);

            long endTime = System.currentTimeMillis();

            if (!result.isSuccess()) {
                return NodeExecutionResult.failure(
                        "Tool execution failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"));
            }

            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            if (result.getData() != null) {
                outputs.putAll(result.getData());
            }
            outputs.put("_toolName", toolName);
            outputs.put("_executionTime", endTime - startTime);

            log.debug("Tool node executed successfully: nodeId={}, latency={}ms",
                    node.getId(), endTime - startTime);

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("Tool node execution failed: nodeId={}", node.getId(), e);
            return NodeExecutionResult.failure(e.getMessage(), getStackTrace(e));
        }
    }

    /**
     * 解析参数中的变量
     */
    private Map<String, Object> resolveParameters(Map<String, Object> params, NodeExecutionContext context) {
        Map<String, Object> resolved = new HashMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                String strValue = (String) value;
                // 解析 ${variable} 格式的变量
                if (strValue.startsWith("${") && strValue.endsWith("}")) {
                    String varName = strValue.substring(2, strValue.length() - 1).trim();
                    Object varValue = context.getVariable(varName);
                    if (varValue == null) {
                        varValue = context.getInput(varName);
                    }
                    resolved.put(entry.getKey(), varValue);
                } else {
                    resolved.put(entry.getKey(), strValue);
                }
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
