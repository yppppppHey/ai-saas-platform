package com.aisaas.common.ai.agent.template;

import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowEdge;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据分析Agent模板
 * 帮助用户分析数据、生成报告和可视化建议
 */
@Component
public class DataAnalysisAgentTemplate implements AgentTemplate {

    @Override
    public String getName() {
        return "数据分析助手";
    }

    @Override
    public String getDescription() {
        return "帮助用户分析数据、生成报告和可视化建议。适用于数据处理、统计分析、趋势预测等场景。";
    }

    @Override
    public String getType() {
        return "data_analysis";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> getTags() {
        return Arrays.asList("数据分析", "统计", "报告", "可视化");
    }

    @Override
    public WorkflowDefinition createWorkflow() {
        String workflowId = "data-analysis-" + UUID.randomUUID().toString().substring(0, 8);

        // 创建节点
        WorkflowNode startNode = WorkflowNode.createStartNode("start", "开始");

        WorkflowNode loadDataNode = WorkflowNode.builder()
                .id("load_data")
                .name("加载数据")
                .type(WorkflowNode.TYPE_TOOL)
                .config(new HashMap<String, Object>() {{
                    put("toolName", "data_loader");
                    put("toolParams", new HashMap<String, Object>() {{
                        put("source", "${dataSource}");
                        put("format", "${dataFormat}");
                    }});
                }})
                .build();

        WorkflowNode analyzeNode = WorkflowNode.builder()
                .id("analyze")
                .name("数据分析和洞察")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位专业的数据分析师。请分析提供的数据，提取关键洞察和模式。");
                    put("prompt", "请分析以下数据：\n\n数据源：${dataSource}\n数据样本：\n${load_data.sample}\n\n请提供：\n1. 数据概述（数据量、字段类型等）\n2. 关键统计指标\n3. 发现的模式或趋势\n4. 异常值或特殊情况\n5. 进一步分析的建议");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode reportNode = WorkflowNode.builder()
                .id("generate_report")
                .name("生成分析报告")
                .type(WorkflowNode.TYPE_LLM)
                .config(new HashMap<String, Object>() {{
                    put("systemPrompt", "你是一位专业的数据报告撰写专家。请基于分析结果生成结构化的数据报告。");
                    put("prompt", "基于以下数据分析结果，生成一份专业的数据分析报告：\n\n分析洞察：\n${analyze.content}\n\n原始数据信息：\n数据源：${dataSource}\n数据格式：${dataFormat}\n\n请生成一份结构化的数据报告，包含以下部分：\n1. 执行摘要\n2. 数据概况\n3. 关键发现\n4. 详细分析\n5. 可视化建议（图表类型建议）\n6. 结论与建议\n\n请使用专业的数据分析师语言风格。");
                    put("provider", "openai");
                    put("model", "gpt-4");
                }})
                .build();

        WorkflowNode endNode = WorkflowNode.createEndNode("end", "结束");

        // 创建边
        List<WorkflowEdge> edges = Arrays.asList(
                WorkflowEdge.createDefaultEdge("e1", "start", "load_data"),
                WorkflowEdge.createDefaultEdge("e2", "load_data", "analyze"),
                WorkflowEdge.createDefaultEdge("e3", "analyze", "generate_report"),
                WorkflowEdge.createDefaultEdge("e4", "generate_report", "end")
        );

        // 构建工作流定义
        return WorkflowDefinition.builder()
                .id(workflowId)
                .name("数据分析助手")
                .description("帮助用户分析数据、生成报告和可视化建议")
                .version("1.0.0")
                .startNodeId("start")
                .nodes(Arrays.asList(startNode, loadDataNode, analyzeNode, reportNode, endNode))
                .edges(edges)
                .build();
    }

    @Override
    public Map<String, Object> getDefaultInputs() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("dataSource", "");
        defaults.put("dataFormat", "auto");
        return defaults;
    }

    @Override
    public List<ParameterDef> getParameterDefs() {
        return Arrays.asList(
                new ParameterDef("dataSource", "string", "数据源路径或内容", true, null),
                new ParameterDef("dataFormat", "string", "数据格式(csv, json, excel等)", false, "auto")
        );
    }

    @Override
    public boolean validateInputs(Map<String, Object> inputs) {
        if (inputs == null || !inputs.containsKey("dataSource")) {
            return false;
        }
        Object dataSource = inputs.get("dataSource");
        return dataSource != null && !dataSource.toString().trim().isEmpty();
    }
}
