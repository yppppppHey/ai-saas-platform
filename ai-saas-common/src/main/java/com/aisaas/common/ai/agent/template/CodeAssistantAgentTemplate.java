package com.aisaas.common.ai.agent.template;

import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowEdge;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 代码助手Agent模板
 * 帮助用户编写、审查和优化代码
 */
@Component
public class CodeAssistantAgentTemplate implements AgentTemplate {

    @Override
    public String getName() {
        return "代码助手";
    }

    @Override
    public String getDescription() {
        return "帮助用户编写、审查和优化代码。适用于代码生成、代码审查、性能优化、重构建议等场景。";
    }

    @Override
    public String getType() {
        return "code_assistant";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> getTags() {
        return Arrays.asList("代码", "编程", "审查", "优化", "重构");
    }

    @Override
    public WorkflowDefinition createWorkflow() {
        String workflowId = "code-assistant-" + UUID.randomUUID().toString().substring(0, 8);

        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");

        WorkflowNode analyzeNode = WorkflowNode.builder()
                .id("analyze_request")
                .name("分析用户需求")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位资深的软件架构师。请分析用户的编程需求，明确任务类型和最佳实践。");
                    put("prompt", "用户请求：\n${userRequest}\n\n编程语言：${language}\n\n请分析：\n1. 任务类型（代码生成/代码审查/性能优化/重构等）\n2. 关键需求点\n3. 最佳实践建议\n4. 可能的挑战");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode executeNode = WorkflowNode.builder()
                .id("execute_task")
                .name("执行编程任务")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位专业的程序员。请根据需求生成高质量、规范的代码。");
                    put("prompt", "基于分析结果，请执行以下编程任务：\n\n用户需求：${userRequest}\n语言：${language}\n代码上下文：\n${codeContext}\n\n分析：${analyze_request.content}\n\n请提供：\n1. 完整可运行的代码\n2. 代码说明和注释\n3. 使用示例\n4. 注意事项");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode reviewNode = WorkflowNode.builder()
                .id("code_review")
                .name("代码审查")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位代码审查专家。请从代码质量、性能、安全性等角度进行审查。");
                    put("prompt", "请审查以下代码：\n\n${execute_task.content}\n\n请从以下维度进行评估：\n1. 代码规范性\n2. 性能优化建议\n3. 安全性问题\n4. 可维护性\n5. 改进建议");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        List<WorkflowEdge> edges = Arrays.asList(
                WorkflowEdge.createDefaultEdge("e1", "start", "analyze_request"),
                WorkflowEdge.createDefaultEdge("e2", "analyze_request", "execute_task"),
                WorkflowEdge.createDefaultEdge("e3", "execute_task", "code_review"),
                WorkflowEdge.createDefaultEdge("e4", "code_review", "end")
        );

        return WorkflowDefinition.builder()
                .id(workflowId)
                .name("代码助手")
                .description("帮助用户编写、审查和优化代码")
                .version("1.0.0")
                .startNodeId("start")
                .nodes(Arrays.asList(startNode, analyzeNode, executeNode, reviewNode, endNode))
                .edges(edges)
                .build();
    }

    @Override
    public Map<String, Object> getDefaultInputs() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("userRequest", "");
        defaults.put("language", "Java");
        defaults.put("codeContext", "");
        return defaults;
    }

    @Override
    public List<ParameterDef> getParameterDefs() {
        return Arrays.asList(
                new ParameterDef("userRequest", "string", "用户的编程需求", true, null),
                new ParameterDef("language", "string", "编程语言", false, "Java"),
                new ParameterDef("codeContext", "string", "现有代码上下文", false, "")
        );
    }

    @Override
    public boolean validateInputs(Map<String, Object> inputs) {
        if (inputs == null || !inputs.containsKey("userRequest")) {
            return false;
        }
        Object request = inputs.get("userRequest");
        return request != null && !request.toString().trim().isEmpty();
    }
}
