package com.aisaas.common.ai.agent.template;

import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowEdge;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 搜索增强问答Agent模板
 * 通过搜索获取相关信息后回答用户问题
 */
@Component
public class SearchQaAgentTemplate implements AgentTemplate {

    @Override
    public String getName() {
        return "搜索增强问答";
    }

    @Override
    public String getDescription() {
        return "通过搜索获取相关信息，然后基于搜索结果回答用户问题。适用于需要实时信息或广泛知识的问题。";
    }

    @Override
    public String getType() {
        return "search_qa";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> getTags() {
        return Arrays.asList("搜索", "问答", "RAG", "实时信息");
    }

    @Override
    public WorkflowDefinition createWorkflow() {
        String workflowId = "search-qa-" + UUID.randomUUID().toString().substring(0, 8);

        // 创建节点
        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");

        WorkflowNode searchNode = WorkflowNode.builder()
                .id("search")
                .name("信息搜索")
                .type(WorkflowNode.TYPE_TOOL)
                .config(new HashMap<String, Object>() {{
                    put("toolName", "web_search");
                    put("toolParams", new HashMap<String, Object>() {{
                        put("query", "${question}");
                        put("limit", 5);
                    }});
                }})
                .build();

        WorkflowNode analyzeNode = WorkflowNode.builder()
                .id("analyze")
                .name("分析搜索结果")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位专业的信息分析专家。请分析搜索结果，提取关键信息。");
                    put("prompt", "用户问题：${question}\n\n搜索结果：\n${search.result}\n\n请分析以上搜索结果，提取出与问题相关的关键信息，并整理成结构化的格式。");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode answerNode = WorkflowNode.builder()
                .id("answer")
                .name("生成回答")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位知识渊博的AI助手。请基于提供的信息回答用户的问题。");
                    put("prompt", "用户问题：${question}\n\n基于搜索得到的关键信息：\n${analyze.content}\n\n请根据以上信息，为用户提供一个完整、准确、有帮助的回答。如果信息不足以回答问题，请明确说明。");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        // 创建边
        List<WorkflowEdge> edges = Arrays.asList(
                WorkflowEdge.createDefaultEdge("e1", "start", "search"),
                WorkflowEdge.createDefaultEdge("e2", "search", "analyze"),
                WorkflowEdge.createDefaultEdge("e3", "analyze", "answer"),
                WorkflowEdge.createDefaultEdge("e4", "answer", "end")
        );

        // 构建工作流定义
        return WorkflowDefinition.builder()
                .id(workflowId)
                .name("搜索增强问答")
                .description("通过搜索获取相关信息后回答用户问题")
                .version("1.0.0")
                .startNodeId("start")
                .nodes(Arrays.asList(startNode, searchNode, analyzeNode, answerNode, endNode))
                .edges(edges)
                .build();
    }

    @Override
    public Map<String, Object> getDefaultInputs() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("question", "");
        return defaults;
    }

    @Override
    public List<ParameterDef> getParameterDefs() {
        return Arrays.asList(
                new ParameterDef("question", "string", "用户的问题", true, null)
        );
    }

    @Override
    public boolean validateInputs(Map<String, Object> inputs) {
        if (inputs == null || !inputs.containsKey("question")) {
            return false;
        }
        Object question = inputs.get("question");
        return question != null && !question.toString().trim().isEmpty();
    }
}
