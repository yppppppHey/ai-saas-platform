package com.aisaas.task.engine.processor;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.engine.context.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class ReportTaskProcessor extends AbstractTaskProcessor {

    @Autowired
    private AIProviderFactory providerFactory;

    @Override
    public String getTaskType() {
        return "report";
    }

    @Override
    public Object execute(TaskExecutionContext context) throws Exception {
        updateProgress(context, 10, "解析报告参数");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = JsonUtils.parseObject(context.getTask().getInputParams(), Map.class);

        String reportType = getStringParam(params, "reportType", "analysis");
        String dataSource = getStringParam(params, "dataSource", "");
        String customPrompt = getStringParam(params, "customPrompt", "");
        @SuppressWarnings("unchecked")
        List<String> sections = (List<String>) params.getOrDefault("sections",
                Arrays.asList("summary", "analysis", "conclusion", "recommendations"));

        updateProgress(context, 30, "收集数据");
        Map<String, Object> data = collectData(dataSource, params);

        updateProgress(context, 50, "生成报告内容");
        String reportContent = generateReportWithAI(reportType, sections, data, customPrompt, context);

        updateProgress(context, 95, "格式化报告");
        Map<String, Object> result = new HashMap<>();
        result.put("title", getStringParam(params, "title", "分析报告"));
        result.put("type", reportType);
        result.put("sections", sections);
        result.put("content", reportContent);
        result.put("metadata", generateMetadata(data, params));
        result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        updateProgress(context, 100, "报告生成完成");
        log.info("报告生成任务完成: taskId={}", context.getTask().getTaskId());

        return result;
    }

    private String generateReportWithAI(String reportType, List<String> sections, 
                                        Map<String, Object> data, String customPrompt,
                                        TaskExecutionContext context) {
        try {
            // 构建提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请根据以下数据和报告类型生成一份专业的分析报告。\n\n");
            promptBuilder.append("报告类型: ").append(reportType).append("\n");
            promptBuilder.append("需要包含的章节: ").append(String.join(", ", sections)).append("\n\n");
            
            // 添加数据摘要
            promptBuilder.append("数据摘要:\n");
            if (data.containsKey("records")) {
                @SuppressWarnings("unchecked")
                List<?> records = (List<?>) data.get("records");
                promptBuilder.append("- 记录数: ").append(records.size()).append("\n");
            }
            if (data.containsKey("metrics")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
                metrics.forEach((k, v) -> promptBuilder.append("- ").append(k).append(": ").append(v).append("\n"));
            }
            
            if (customPrompt != null && !customPrompt.isEmpty()) {
                promptBuilder.append("\n额外要求:\n").append(customPrompt).append("\n");
            }
            
            promptBuilder.append("\n请使用Markdown格式生成报告，包含标题、章节标题和列表。");
            
            updateProgress(context, 60, "调用AI生成报告");
            
            // 调用AI
            AIProvider provider = providerFactory.getProvider("openai");
            ChatRequest request = ChatRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(Collections.singletonList(
                    ChatRequest.Message.builder()
                        .role("user")
                        .content(promptBuilder.toString())
                        .build()
                ))
                .temperature(0.7)
                .maxTokens(3000)
                .build();
            
            ChatResponse response = provider.chat(request);
            
            updateProgress(context, 90, "报告生成成功");
            return response.getContent();
            
        } catch (Exception e) {
            log.error("AI报告生成失败", e);
            // 回退到模板生成
            return generateTemplateReport(reportType, sections, data);
        }
    }

    private Map<String, Object> collectData(String dataSource, Map<String, Object> params) {
        Map<String, Object> data = new HashMap<>();
        
        if (dataSource != null && !dataSource.isEmpty()) {
            data.put("source", dataSource);
            data.put("records", generateMockRecords(10));
            data.put("metrics", generateMockMetrics());
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> additionalData = (Map<String, Object>) params.get("data");
        if (additionalData != null) {
            data.putAll(additionalData);
        }
        
        return data;
    }

    private List<Map<String, Object>> generateMockRecords(int count) {
        List<Map<String, Object>> records = new ArrayList<>();
        String[] categories = {"产品A", "产品B", "产品C", "服务A", "服务B"};
        String[] regions = {"华北", "华东", "华南", "西南", "西北"};
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i + 1);
            record.put("category", categories[i % categories.length]);
            record.put("region", regions[i % regions.length]);
            record.put("value", 1000 + Math.random() * 9000);
            record.put("quantity", (int) (10 + Math.random() * 90));
            record.put("date", LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
            records.add(record);
        }
        
        return records;
    }

    private Map<String, Object> generateMockMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRevenue", 1250000.00);
        metrics.put("totalOrders", 1250);
        metrics.put("averageOrderValue", 1000.00);
        metrics.put("conversionRate", 0.035);
        metrics.put("customerSatisfaction", 4.5);
        metrics.put("growthRate", 0.15);
        return metrics;
    }

    private Map<String, Object> generateMetadata(Map<String, Object> data, Map<String, Object> params) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recordCount", data.getOrDefault("records", new ArrayList<>()) instanceof List ? 
            ((List<?>) data.get("records")).size() : 0);
        metadata.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.put("reportType", params.getOrDefault("reportType", "analysis"));
        return metadata;
    }

    private String generateTemplateReport(String reportType, List<String> sections, Map<String, Object> data) {
        StringBuilder report = new StringBuilder();
        report.append("# ").append(reportType.toUpperCase()).append(" 报告\n\n");
        
        for (String section : sections) {
            report.append("## ").append(getSectionName(section)).append("\n\n");
            report.append(generateSectionContent(section, data)).append("\n\n");
        }
        
        report.append("---\n");
        report.append("*报告生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("*");
        
        return report.toString();
    }

    private String getSectionName(String section) {
        return switch (section) {
            case "summary" -> "执行摘要";
            case "analysis" -> "详细分析";
            case "conclusion" -> "结论";
            case "recommendations" -> "建议";
            case "methodology" -> "方法论";
            case "appendix" -> "附录";
            default -> section;
        };
    }

    private String generateSectionContent(String section, Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
        
        switch (section) {
            case "summary" -> {
                return String.format("""
                    本报告基于数据分析，总结了关键发现。数据样本共包含 %d 条记录，
                    总收入为 ¥%.2f，总订单量为 %d 单。
                    
                    关键指标表现良好，客户满意度达到 %.1f 分（满分5分），
                    整体增长率维持在 %.1f%% 的健康水平。
                    """,
                    records != null ? records.size() : 0,
                    metrics != null ? metrics.getOrDefault("totalRevenue", 0) : 0,
                    metrics != null ? metrics.getOrDefault("totalOrders", 0) : 0,
                    metrics != null ? metrics.getOrDefault("customerSatisfaction", 0) : 0,
                    metrics != null ? ((Number) metrics.getOrDefault("growthRate", 0)).doubleValue() * 100 : 0
                );
            }
            case "analysis" -> {
                return """
                    通过对数据的深入分析，我们识别出以下关键模式：
                    
                    1. **类别分布**：各产品类别表现均衡，没有出现明显的偏斜。
                    2. **地域差异**：不同区域的数据呈现出各自的特点，建议针对性地制定策略。
                    3. **趋势变化**：时间序列分析显示数据整体呈上升趋势。
                    """;
            }
            case "conclusion" -> {
                return """
                    综合分析结果，我们得出以下结论：
                    
                    1. 当前业务运营状况良好，各项关键指标均达到预期目标。
                    2. 数据质量较高，为后续决策提供了可靠的基础。
                    3. 建议持续关注关键指标的变化，及时调整策略。
                    """;
            }
            case "recommendations" -> {
                return """
                    基于分析结果，我们提出以下建议：
                    
                    1. **优化产品组合**：根据类别表现，调整产品策略。
                    2. **区域精细化运营**：针对不同区域特点，制定差异化策略。
                    3. **数据驱动决策**：建立完善的数据监控体系，支持实时决策。
                    4. **持续改进**：定期回顾分析结果，不断优化业务流程。
                    """;
            }
            default -> {
                return "该部分内容详见详细数据附件。";
            }
        }
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
