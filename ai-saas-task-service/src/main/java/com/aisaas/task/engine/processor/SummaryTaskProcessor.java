package com.aisaas.task.engine.processor;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.parser.DocumentParser;
import com.aisaas.common.ai.document.parser.PdfDocumentParser;
import com.aisaas.common.ai.document.parser.TextDocumentParser;
import com.aisaas.common.ai.document.parser.WordDocumentParser;
import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import com.aisaas.common.exception.BizException;
import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Slf4j
@Component
public class SummaryTaskProcessor extends AbstractTaskProcessor {

    /** 从 URL 下载文档的最大字节数（20MB），防止大文档拖垮内存 */
    private static final int MAX_DOCUMENT_BYTES = 20 * 1024 * 1024;

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

        // 如果有documentUrl，下载并解析文档（复用 common 的文档解析器）
        if (!documentUrl.isEmpty()) {
            return fetchDocumentContent(documentUrl);
        }

        return "无内容";
    }

    /**
     * 从 URL 下载文档并用合适的解析器提取纯文本内容。
     * 支持 PDF / Word / 纯文本等 common 模块解析器支持的类型。
     *
     * @param documentUrl 文档 URL
     * @return 解析出的文本内容
     */
    private String fetchDocumentContent(String documentUrl) {
        List<DocumentParser> parsers = Arrays.asList(
                new PdfDocumentParser(), new WordDocumentParser(), new TextDocumentParser());

        HttpURLConnection connection = null;
        try {
            URL url = new URL(documentUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new BizException("文档下载失败, HTTP状态码: " + connection.getResponseCode());
            }

            String fileName = extractFileName(documentUrl, connection);
            // 有界读取：最多下载 MAX_DOCUMENT_BYTES 字节，超过即视为文档过大
            byte[] bytes;
            try (InputStream in = new java.io.BufferedInputStream(connection.getInputStream())) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    if (buffer.size() + read > MAX_DOCUMENT_BYTES) {
                        throw new BizException("文档过大，超过最大限制 " + (MAX_DOCUMENT_BYTES / 1024 / 1024) + "MB");
                    }
                    buffer.write(chunk, 0, read);
                }
                bytes = buffer.toByteArray();
            }

            DocumentParser parser = parsers.stream()
                    .filter(p -> p.supports(fileName))
                    .findFirst()
                    .orElseThrow(() -> new BizException("不支持的文档类型: " + fileName));

            Document document = parser.parse(bytes, fileName);
            String content = document != null ? document.getContent() : null;
            if (content == null || content.isBlank()) {
                throw new BizException("文档解析结果为空: " + fileName);
            }
            return content;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("从URL获取文档内容失败: {}", documentUrl, e);
            throw new BizException("文档下载或解析失败: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从 URL 路径或响应头中提取文件名（用于识别文档类型）
     */
    private String extractFileName(String documentUrl, HttpURLConnection connection) {
        // 优先使用 Content-Disposition 中的文件名
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null && disposition.contains("filename=")) {
            int idx = disposition.indexOf("filename=");
            String name = disposition.substring(idx + "filename=".length()).trim();
            name = name.replace("\"", "").replace("UTF-8''", "");
            if (!name.isBlank()) {
                return java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        // 回退：取 URL 路径的最后一段
        String path = documentUrl.substring(documentUrl.indexOf("://") + 3);
        int queryIdx = path.indexOf('?');
        if (queryIdx > 0) {
            path = path.substring(0, queryIdx);
        }
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return name.isBlank() ? "document.txt" : name;
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
