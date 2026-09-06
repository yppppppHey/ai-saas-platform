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
 * 条件节点执行器
 * 根据条件表达式决定流程走向
 */
@Slf4j
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_CONDITION;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing condition node: {}", node.getId());

        try {
            // 获取条件表达式
            String condition = node.getConfig("condition", "").toString();
            if (condition.isEmpty()) {
                return NodeExecutionResult.failure("Condition expression not specified");
            }

            // 评估条件
            boolean result = evaluateCondition(condition, context);

            log.debug("Condition evaluated: {} = {}", condition, result);

            // 返回结果
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("condition", result);
            outputs.put("conditionExpression", condition);

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("Condition node execution failed: nodeId={}", node.getId(), e);
            return NodeExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * 评估条件表达式
     * 支持简单的变量比较和逻辑运算
     */
    private boolean evaluateCondition(String condition, NodeExecutionContext context) {
        // 去除多余空格
        String expr = condition.trim();

        // 处理括号
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evaluateCondition(expr.substring(1, expr.length() - 1), context);
        }

        // 处理逻辑运算符
        if (expr.contains(" && ")) {
            String[] parts = expr.split(" && ");
            for (String part : parts) {
                if (!evaluateCondition(part.trim(), context)) {
                    return false;
                }
            }
            return true;
        }

        if (expr.contains(" || ")) {
            String[] parts = expr.split(" || ");
            for (String part : parts) {
                if (evaluateCondition(part.trim(), context)) {
                    return true;
                }
            }
            return false;
        }

        // 处理比较运算符
        if (expr.contains(" == ")) {
            String[] parts = expr.split(" == ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseLiteral(parts[1].trim());
            return java.util.Objects.equals(left, right);
        }

        if (expr.contains(" != ")) {
            String[] parts = expr.split(" != ");
            Object left = getValue(parts[0].trim(), context);
            Object right = parseLiteral(parts[1].trim());
            return !java.util.Objects.equals(left, right);
        }

        if (expr.contains(" > ")) {
            String[] parts = expr.split(" > ");
            double left = toNumber(getValue(parts[0].trim(), context));
            double right = toNumber(parseLiteral(parts[1].trim()));
            return left > right;
        }

        if (expr.contains(" < ")) {
            String[] parts = expr.split(" < ");
            double left = toNumber(getValue(parts[0].trim(), context));
            double right = toNumber(parseLiteral(parts[1].trim()));
            return left < right;
        }

        if (expr.contains(" >= ")) {
            String[] parts = expr.split(" >= ");
            double left = toNumber(getValue(parts[0].trim(), context));
            double right = toNumber(parseLiteral(parts[1].trim()));
            return left >= right;
        }

        if (expr.contains(" <= ")) {
            String[] parts = expr.split(" <= ");
            double left = toNumber(getValue(parts[0].trim(), context));
            double right = toNumber(parseLiteral(parts[1].trim()));
            return left <= right;
        }

        // 简单的真值判断
        Object value = getValue(expr, context);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && !value.toString().isEmpty();
    }

    /**
     * 获取变量值
     */
    private Object getValue(String expr, NodeExecutionContext context) {
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            // 字符串字面量
            return expr.substring(1, expr.length() - 1);
        }

        if (expr.startsWith("'") && expr.endsWith("'")) {
            // 单引号字符串
            return expr.substring(1, expr.length() - 1);
        }

        // 尝试从变量中获取
        Object value = context.getVariable(expr);
        if (value == null) {
            value = context.getInput(expr);
        }
        if (value == null) {
            value = context.getContextData(expr);
        }

        return value;
    }

    /**
     * 解析字面量
     */
    private Object parseLiteral(String value) {
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

    /**
     * 转换为数字
     */
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
