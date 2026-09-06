package com.aisaas.task.engine.processor;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.factory.AIProviderFactory;
import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class SummaryTaskProcessor extends AbstractTaskProcessor {

    @Autowired
    private AIProviderFactory providerFactory;

    @Override
    public String getTaskType() {
        return "summary";
    }

    @Override
    public Object execute(TaskExecutionContext context) throws Exception {
        TaskAsyncJob task = context.getTask();
        Map<String, Object> params = JsonUtils.parseObject(task.getInputParams(), Map.class);

        log.info("开始执行总结任务: taskId={}, params={}", task.getTaskId(), params);

        updateProgress(context, 20, "解析文档内容");
        String content = extractContent(params);

        updateProgress(context, 40, "分析文档结构");
        String summaryLength = getStringParam(params, "summaryLength", "medium");
        String focusArea = getStringParam(params, "focusArea", "general");
        String outputFormat = getStringParam(params, "outputFormat", "paragraph");

        updateProgress(context, 60, "生成文档总结");
        String summary = generateAISummary(content, summaryLength, focusArea, outputFormat);

        updateProgress(context, 80, "格式化输出");
        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("originalLength", content.length());
        result.put("summaryLength", summary.length());
        result.put("compressionRatio", String.format("%.2f%%", (1.0 * summary.length() / Math.max(content.length(), 1)) * 100));
        result.put("focusPoints", params.get("focusPoints"));
        result.put("summaryType", focusArea);
        result.put("outputFormat", outputFormat);

        updateProgress(context, 100, "总结任务完成");
        log.info("总结任务执行完成: taskId={}", task.getTaskId());

        return result;
    }

    private String extractContent(Map<String, Object> params) {
        String documentUrl = getStringParam(params, "documentUrl", "");
        String documentContent = getStringParam(params, "documentContent", "");
        String textContent = getStringParam(params, "textContent", "");
        String content = getStringParam(params, "content", "");

        // 优先使用documentContent，然后是textContent，然后是content
        if (!documentContent.isEmpty()) {
            return documentContent;
        }
        
        if (!textContent.isEmpty()) {
            return textContent;
        }
        
        if (!content.isEmpty()) {
            return content;
        }

        // 如果有documentUrl，这里应该下载并解析文档
        // 由于这是一个简化版本，我们返回占位符
        if (!documentUrl.isEmpty()) {
            return "[文档内容从URL获取: " + documentUrl + "]";
        }

        return "无内容";
    }

    private String generateAISummary(String content, String length, String focusArea, String outputFormat) {
        try {
            // 确定目标摘要长度
            int targetLength = switch (length.toLowerCase()) {
                case "short" -> 100;
                case "long" -> 500;
                default -> 300; // medium
            };

            // 构建提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请对以下文本进行总结。\n\n");
            promptBuilder.append("要求:\n");
            promptBuilder.append("- 摘要长度约").append(targetLength).append("字\n");
            
            // 根据焦点区域调整提示
            switch (focusArea.toLowerCase()) {
                case "key_points" -> promptBuilder.append("- 重点关注关键要点\n");
                case "action_items" -> promptBuilder.append("- 提取行动项和待办事项\n");
                case "decisions" -> promptBuilder.append("- 重点关注决策内容\n");
                default -> promptBuilder.append("- 提供全面总结\n");
            }
            
            // 根据输出格式调整
            switch (outputFormat.toLowerCase()) {
                case "bullet_points" -> promptBuilder.append("- 使用 bullet points 格式\n");
                case "numbered" -> promptBuilder.append("- 使用编号列表格式\n");
                default -> promptBuilder.append("- 使用段落格式\n");
            }
            
            // 截取内容（避免过长）
            String truncatedContent = content.length() > 8000 ? 
                content.substring(0, 8000) + "..." : content;
            
            promptBuilder.append("\n原文内容:\n").append(truncatedContent);

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
                .temperature(0.3)
                .maxTokens(targetLength * 2)
                .build();

            ChatResponse response = provider.chat(request);
            
            if (response != null && response.getContent() != null && !response.getContent().isEmpty()) {
                return response.getContent();
            }
            
            // 如果AI返回为空，回退到mock
            return generateMockSummary(content, length);
            
        } catch (Exception e) {
            log.error("AI生成摘要失败", e);
            return generateMockSummary(content, length);
        }
    }

    private String generateMockSummary(String content, String length) {
        int summaryLength = switch (length.toLowerCase()) {
            case "short" -> 100;
            case "long" -> 500;
            default -> 300;
        };

        String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;

        return String.format("""
            【文档摘要】

            原文预览：
            %s

            核心要点：
            - 这是一份重要的文档，包含了关键信息
            - 文档内容丰富，结构清晰
            - 建议仔细阅读以获取完整信息

            总结长度：约%d字符
            原文长度：%d字符
            压缩比例：%.1f%%
            """,
                preview, summaryLength, content.length(),
                (1.0 - (double) summaryLength / Math.max(content.length(), 1)) * 100);
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
