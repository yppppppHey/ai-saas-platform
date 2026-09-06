package com.aisaas.common.ai.agent.node;

import com.aisaas.common.ai.agent.tool.ToolRegistry;
import com.aisaas.common.ai.provider.factory.AIProviderFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行上下文
 * 包含节点执行所需的所有上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionContext {

    /**
     * 工作流实例ID
     */
    private String instanceId;

    /**
     * 当前节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点输入参数
     */
    @Builder.Default
    private Map<String, Object> inputs = new HashMap<>();

    /**
     * 工作流全局变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 执行上下文数据
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * AI Provider工厂
     */
    private AIProviderFactory providerFactory;

    /**
     * 工具注册表
     */
    private ToolRegistry toolRegistry;

    /**
     * 获取输入参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key) {
        return (T) inputs.get(key);
    }

    /**
     * 获取输入参数（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key, T defaultValue) {
        Object value = inputs.get(key);
        return value != null ? (T) value : defaultValue;
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
     * 设置上下文数据
     */
    public void setContextData(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 获取上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key) {
        return (T) context.get(key);
    }

    /**
     * 添加变量
     */
    public void addVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 批量添加变量
     */
    public void addVariables(Map<String, Object> vars) {
        variables.putAll(vars);
    }
}
