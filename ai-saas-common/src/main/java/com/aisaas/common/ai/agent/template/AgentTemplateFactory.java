package com.aisaas.common.ai.agent.template;

import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowEdge;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent模板工厂
 * 创建和管理内置Agent模板
 */
@Component
public class AgentTemplateFactory {

    /**
     * 搜索增强问答Agent模板
     * 先搜索获取信息，然后基于搜索结果回答问题
     */
    public AgentTemplate createSearchQAAgentTemplate() {
        String templateId = "search_qa_agent";

        // 构建工作流定义
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id(templateId + "_workflow")
                .name("搜索问答Agent")
                .description("先搜索后回答的Agent")
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        // 添加节点
        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");
        WorkflowNode searchNode = WorkflowNode.createToolNode("search", "搜索", "web_search",
                Map.of("query", "${userQuery}"));
        WorkflowNode llmNode = WorkflowNode.createLlmNode("answer", "回答问题",
                "基于以下搜索结果回答用户问题:\n${search.results}\n\n用户问题: ${userQuery}",
                "gpt-4", "openai");
        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        workflow.getNodes().addAll(Arrays.asList(startNode, searchNode, llmNode, endNode));

        // 添加边
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e1", "start", "search"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e2", "search", "answer"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e3", "answer", "end"));

        // 构建模板
        AgentTemplate template = AgentTemplate.builder()
                .id(templateId)
                .name("搜索增强问答Agent")
                .description("自动搜索网络信息并基于搜索结果回答用户问题的Agent")
                .category("question_answering")
                .version("1.0.0")
                .workflowDefinition(workflow)
                .systemPrompt("你是一个智能助手，擅长搜索信息并基于搜索结果回答用户问题。你会先进行网络搜索获取相关信息，然后结合搜索结果给出准确、全面的回答。")
                .userPromptTemplate("用户问题: ${userQuery}")
                .inputParameters(Map.of(
                        "userQuery", AgentTemplate.ParameterDef.builder()
                                .name("userQuery")
                                .type("string")
                                .description("用户的问题或查询")
                                .required(true)
                                .build()
                ))
                .outputs(Map.of(
                        "answer", AgentTemplate.OutputDef.builder()
                                .name("answer")
                                .type("string")
                                .description("Agent的回答")
                                .build()
                ))
                .requiredTools(Arrays.asList("web_search"))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .status("PUBLISHED")
                .build();

        return template;
    }

    /**
     * 数据分析Agent模板
     * 分析数据并生成洞察和报告
     */
    public AgentTemplate createDataAnalysisAgentTemplate() {
        String templateId = "data_analysis_agent";

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id(templateId + "_workflow")
                .name("数据分析Agent")
                .description("自动分析数据并生成报告的Agent")
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        // 构建节点和边
        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");
        WorkflowNode preprocessNode = WorkflowNode.createToolNode("preprocess", "数据预处理", "data_preprocessor",
                Map.of("data", "${inputData}", "operations", "${preprocessConfig}"));
        WorkflowNode analyzeNode = WorkflowNode.createLlmNode("analyze", "数据分析",
                "请分析以下数据并提取关键洞察:\n\n数据:\n${preprocess.output}\n\n分析要求:\n${analysisRequirements}",
                "gpt-4", "openai");
        WorkflowNode reportNode = WorkflowNode.createLlmNode("report", "生成报告",
                "基于以下分析结果生成一份专业的数据报告:\n\n分析结果:\n${analyze.output}\n\n报告要求:\n${reportRequirements}",
                "gpt-4", "openai");
        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        workflow.getNodes().addAll(Arrays.asList(startNode, preprocessNode, analyzeNode, reportNode, endNode));

        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e1", "start", "preprocess"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e2", "preprocess", "analyze"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e3", "analyze", "report"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e4", "report", "end"));

        AgentTemplate template = AgentTemplate.builder()
                .id(templateId)
                .name("数据分析Agent")
                .description("自动分析数据并生成专业洞察和报告的Agent")
                .category("data_analysis")
                .version("1.0.0")
                .workflowDefinition(workflow)
                .systemPrompt("你是一位专业的数据分析师，擅长从数据中提取有价值的洞察并生成清晰、专业的分析报告。你能够识别数据趋势、异常和模式，并以易于理解的方式呈现分析结果。")
                .userPromptTemplate("数据: ${inputData}\n\n分析要求: ${analysisRequirements}")
                .inputParameters(Map.of(
                        "inputData", AgentTemplate.ParameterDef.builder()
                                .name("inputData")
                                .type("object")
                                .description("要分析的数据")
                                .required(true)
                                .build(),
                        "analysisRequirements", AgentTemplate.ParameterDef.builder()
                                .name("analysisRequirements")
                                .type("string")
                                .description("分析要求")
                                .required(false)
                                .defaultValue("请提供全面的数据分析，包括趋势、异常和关键指标")
                                .build()
                ))
                .outputs(Map.of(
                        "insights", AgentTemplate.OutputDef.builder()
                                .name("insights")
                                .type("string")
                                .description("数据洞察")
                                .build(),
                        "report", AgentTemplate.OutputDef.builder()
                                .name("report")
                                .type("string")
                                .description("分析报告")
                                .build()
                ))
                .requiredTools(Arrays.asList("data_preprocessor"))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .status("PUBLISHED")
                .build();

        return template;
    }

    /**
     * 代码助手Agent模板
     * 生成、审查和优化代码
     */
    public AgentTemplate createCodeAssistantAgentTemplate() {
        String templateId = "code_assistant_agent";

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id(templateId + "_workflow")
                .name("代码助手Agent")
                .description("协助代码生成、审查和优化的Agent")
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        // 构建节点和边
        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");
        WorkflowNode analyzeNode = WorkflowNode.createLlmNode("analyze", "分析需求",
                "请分析以下编程任务或代码:\n\n任务/代码:\n${codeOrTask}\n\n分析要求:\n${analysisType}",
                "gpt-4", "openai");
        WorkflowNode codeNode = WorkflowNode.createToolNode("code", "代码操作", "code_executor",
                Map.of("code", "${codeOrTask}", "operation", "${operationType}", "language", "${language}"));
        WorkflowNode reviewNode = WorkflowNode.createLlmNode("review", "代码审查",
                "请审查以下代码并提供改进建议:\n\n代码:\n${code.output}\n\n原始分析:\n${analyze.output}",
                "gpt-4", "openai");
        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        workflow.getNodes().addAll(Arrays.asList(startNode, analyzeNode, codeNode, reviewNode, endNode));

        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e1", "start", "analyze"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e2", "analyze", "code"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e3", "code", "review"));
        workflow.getEdges().add(WorkflowEdge.createDefaultEdge("e4", "review", "end"));

        AgentTemplate template = AgentTemplate.builder()
                .id(templateId)
                .name("代码助手Agent")
                .description("协助代码生成、审查、优化和重构的智能Agent")
                .category("code_assistant")
                .version("1.0.0")
                .workflowDefinition(workflow)
                .systemPrompt("你是一位经验丰富的软件工程师，擅长代码生成、审查和优化。你能够理解各种编程语言和框架，提供高质量的代码建议，识别潜在问题，并提出改进方案。你注重代码质量、可读性和性能。")
                .userPromptTemplate("任务/代码: ${codeOrTask}\n\n操作类型: ${operationType}\n\n编程语言: ${language}")
                .inputParameters(Map.of(
                        "codeOrTask", AgentTemplate.ParameterDef.builder()
                                .name("codeOrTask")
                                .type("string")
                                .description("代码片段或任务描述")
                                .required(true)
                                .build(),
                        "operationType", AgentTemplate.ParameterDef.builder()
                                .name("operationType")
                                .type("string")
                                .description("操作类型：generate（生成）、review（审查）、optimize（优化）、refactor（重构）")
                                .required(false)
                                .defaultValue("review")
                                .enumValues(Arrays.asList("generate", "review", "optimize", "refactor"))
                                .build(),
                        "language", AgentTemplate.ParameterDef.builder()
                                .name("language")
                                .type("string")
                                .description("编程语言")
                                .required(false)
                                .defaultValue("java")
                                .build()
                ))
                .outputs(Map.of(
                        "analysis", AgentTemplate.OutputDef.builder()
                                .name("analysis")
                                .type("string")
                                .description("代码分析结果")
                                .build(),
                        "suggestions", AgentTemplate.OutputDef.builder()
                                .name("suggestions")
                                .type("string")
                                .description("改进建议")
                                .build(),
                        "improvedCode", AgentTemplate.OutputDef.builder()
                                .name("improvedCode")
                                .type("string")
                                .description("优化后的代码")
                                .build()
                ))
                .requiredTools(Arrays.asList("code_executor"))
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .status("PUBLISHED")
                .build();

        return template;
    }

    /**
     * 获取所有内置模板
     */
    public Map<String, AgentTemplate> getAllTemplates() {
        Map<String, AgentTemplate> templates = new HashMap<>();
        templates.put("search_qa_agent", createSearchQAAgentTemplate());
        templates.put("data_analysis_agent", createDataAnalysisAgentTemplate());
        templates.put("code_assistant_agent", createCodeAssistantAgentTemplate());
        return templates;
    }
}
