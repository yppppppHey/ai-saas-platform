package com.aisaas.common.ai.agent.node.executor;

import com.aisaas.common.ai.agent.node.NodeExecutionContext;
import com.aisaas.common.ai.agent.node.NodeExecutionResult;
import com.aisaas.common.ai.agent.node.NodeExecutor;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环节点执行器
 * 根据条件循环执行循环体内的节点
 */
@Slf4j
@Component
public class LoopNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_LOOP;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing loop node: {}", node.getId());

        try {
            // 获取循环条件
            String condition = node.getConfig("condition", "").toString();
            int maxIterations = node.getConfig("maxIterations", 100);
            String loopVariable = node.getConfig("loopVariable", "i").toString();
            int currentIteration = 0;

            // 从上下文中获取当前循环计数
            Object existingCount = context.getContextData(node.getId() + ".iteration");
            if (existingCount != null) {
                currentIteration = ((Number) existingCount).intValue();
            }

            // 初始化循环变量
            context.addVariable(loopVariable, currentIteration);

            // 评估循环条件
            boolean shouldContinue = evaluateCondition(condition, context);

            log.debug("Loop evaluation: nodeId={}, iteration={}, condition={}, shouldContinue={}",
                    node.getId(), currentIteration, condition, shouldContinue);

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("continue", shouldContinue);
            outputs.put("iteration", currentIteration);
            outputs.put("loopVariable", loopVariable);
            outputs.put("loopVariableValue", currentIteration);

            if (shouldContinue) {
                // 增加迭代计数
                currentIteration++;
                context.setContextData(node.getId() + ".iteration", currentIteration);

                // 检查是否超过最大迭代次数
                if (currentIteration > maxIterations) {
                    log.warn("Loop exceeded max iterations: nodeId={}, maxIterations={}",
                            node.getId(), maxIterations);
                    outputs.put("continue", false);
                    outputs.put("maxIterationsReached", true);
                    outputs.put("_warning", "Loop terminated due to exceeding max iterations");
                }
            } else {
                // 循环结束，清理上下文
                context.setContextData(node.getId() + ".iteration", null);
            }

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("Loop node execution failed: nodeId={}", node.getId(), e);
            return NodeExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * 评估循环条件
     * 支持简单的条件表达式
     */
    private boolean evaluateCondition(String condition, NodeExecutionContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return true; // 无条件时默认继续
        }

        String expr = condition.trim();

        // 处理比较表达式
        if (expr.contains(" < ")) {
            String[] parts = expr.split(" < ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return toNumber(left) < toNumber(right);
        }

        if (expr.contains(" > ")) {
            String[] parts = expr.split(" > ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return toNumber(left) > toNumber(right);
        }

        if (expr.contains(" <= ")) {
            String[] parts = expr.split(" <= ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return toNumber(left) <= toNumber(right);
        }

        if (expr.contains(" >= ")) {
            String[] parts = expr.split(" >= ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return toNumber(left) >= toNumber(right);
        }

        if (expr.contains(" == ")) {
            String[] parts = expr.split(" == ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return java.util.Objects.equals(left, right);
        }

        if (expr.contains(" != ")) {
            String[] parts = expr.split(" != ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseValue(parts[1].trim());
            return !java.util.Objects.equals(left, right);
        }

        // 简单变量真值判断
        Object value = getValue(expr, context);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        return value != null && !value.toString().isEmpty();
    }

    private Object getValue(String expr, NodeExecutionContext context) {
        // 尝试作为变量名获取
        Object value = context.getVariable(expr);
        if (value != null) {
            return value;
        }

        // 尝试作为输入获取
        value = context.getInput(expr);
        if (value != null) {
            return value;
        }

        // 作为字面量解析
        return parseValue(expr);
    }

    private Object parseValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private double toNumber(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
