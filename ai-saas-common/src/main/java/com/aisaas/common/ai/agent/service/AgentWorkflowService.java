package com.aisaas.common.ai.agent.service;

import com.aisaas.common.ai.agent.engine.WorkflowEngine;
import com.aisaas.common.ai.agent.template.AgentTemplate;
import com.aisaas.common.ai.agent.template.AgentTemplateFactory;
import com.aisaas.common.ai.agent.tool.Tool;
import com.aisaas.common.ai.agent.tool.ToolRegistry;
import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent工作流服务
 * 提供Agent工作流的完整管理能力
 */
@Slf4j
@Service
public class AgentWorkflowService {

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private AgentTemplateFactory templateFactory;

    /**
     * 启动工作流实例
     */
    public WorkflowInstance startWorkflow(WorkflowDefinition definition, Map<String, Object> inputs) {
        return workflowEngine.startWorkflow(definition, inputs);
    }

    /**
     * 启动工作流实例（带父实例）
     */
    public WorkflowInstance startWorkflow(WorkflowDefinition definition, Map<String, Object> inputs,
                                       String parentInstanceId) {
        return workflowEngine.startWorkflow(definition, inputs, parentInstanceId);
    }

    /**
     * 从Agent模板启动工作流
     */
    public WorkflowInstance startFromTemplate(String templateId, Map<String, Object> inputs) {
        AgentTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Agent template not found: " + templateId);
        }

        WorkflowDefinition definition = template.getWorkflowDefinition();
        if (definition == null) {
            throw new IllegalStateException("Template has no workflow definition: " + templateId);
        }

        // 合并模板配置和输入
        Map<String, Object> mergedInputs = new HashMap<>();
        if (template.getConfig() != null) {
            mergedInputs.putAll(template.getConfig());
        }
        if (inputs != null) {
            mergedInputs.putAll(inputs);
        }

        return startWorkflow(definition, mergedInputs);
    }

    /**
     * 获取工作流实例
     */
    public WorkflowInstance getInstance(String instanceId) {
        return workflowEngine.getInstance(instanceId);
    }

    /**
     * 获取所有运行中的实例
     */
    public Collection<WorkflowInstance> getRunningInstances() {
        return workflowEngine.getRunningInstances();
    }

    /**
     * 暂停工作流实例
     */
    public void pauseInstance(String instanceId) {
        workflowEngine.pauseInstance(instanceId);
    }

    /**
     * 恢复工作流实例
     */
    public void resumeInstance(String instanceId) {
        workflowEngine.resumeInstance(instanceId);
    }

    /**
     * 取消工作流实例
     */
    public void cancelInstance(String instanceId) {
        workflowEngine.cancelInstance(instanceId);
    }

    // ============ Tool管理 ============

    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        toolRegistry.register(tool);
    }

    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return toolRegistry.getTool(name);
    }

    /**
     * 获取所有工具
     */
    public Collection<Tool> getAllTools() {
        return toolRegistry.getAllTools();
    }

    /**
     * 搜索工具
     */
    public List<Tool> searchTools(String keyword) {
        return toolRegistry.searchTools(keyword);
    }

    /**
     * 执行工具
     */
    public Tool.ToolResult executeTool(String toolName, Map<String, Object> params) {
        Tool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return Tool.ToolResult.failure("Tool not found: " + toolName);
        }

        // 验证参数
        Tool.ValidationResult validation = tool.validate(params);
        if (!validation.isValid()) {
            String errorMsg = validation.getErrors().stream()
                    .map(e -> e.getField() + ": " + e.getMessage())
                    .collect(Collectors.joining(", "));
            return Tool.ToolResult.failure("参数验证失败: " + errorMsg);
        }

        return tool.execute(params);
    }

    // ============ 模板管理 ============

    /**
     * 获取Agent模板
     */
    public AgentTemplate getTemplate(String templateId) {
        return templateFactory.getAllTemplates().get(templateId);
    }

    /**
     * 获取所有Agent模板
     */
    public Map<String, AgentTemplate> getAllTemplates() {
        return templateFactory.getAllTemplates();
    }

    /**
     * 按分类获取模板
     */
    public List<AgentTemplate> getTemplatesByCategory(String category) {
        return templateFactory.getAllTemplates().values().stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * 搜索模板
     */
    public List<AgentTemplate> searchTemplates(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return templateFactory.getAllTemplates().values().stream()
                .filter(t -> t.getName().toLowerCase().contains(lowerKeyword)
                        || t.getDescription().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }
}
