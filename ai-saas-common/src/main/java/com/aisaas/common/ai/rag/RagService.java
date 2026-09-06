package com.aisaas.common.ai.rag;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.prompt.PromptTemplate;
import com.aisaas.common.ai.prompt.PromptTemplateManager;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import com.aisaas.common.ai.vector.VectorStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) 服务
 * 实现检索增强生成功能
 */
@Slf4j
@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private PromptTemplateManager promptTemplateManager;

    // 默认检索数量
    private static final int DEFAULT_RETRIEVAL_TOP_K = 5;
    // 默认相似度阈值
    private static final double DEFAULT_MIN_SIMILARITY = 0.7;
    // 默认最大上下文长度
    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 4000;
    // 默认RAG问答场景类型
    private static final String DEFAULT_RAG_SCENE_TYPE = "rag_qa";

    /**
     * RAG 问答
     *
     * @param request RAG请求
     * @return RAG响应
     */
    public RagResponse ragQuery(RagRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 检索相关文档
            log.debug("Retrieving documents for query: {}", request.getQuery());
            List<RetrievalResult> retrievals = retrieveDocuments(request);

            // 2. 构建上下文
            String context = buildContext(retrievals, request.getMaxContextLength() != null
                    ? request.getMaxContextLength() : DEFAULT_MAX_CONTEXT_LENGTH);

            // 3. 构建提示词
            String prompt = buildPrompt(request.getQuery(), context, request.getSystemPrompt());

            // 4. 调用LLM生成回答
            ChatResponse chatResponse = generateResponse(prompt, request);

            long endTime = System.currentTimeMillis();

            return RagResponse.builder()
                    .answer(chatResponse.getContent())
                    .retrievals(retrievals)
                    .context(context)
                    .prompt(prompt)
                    .provider(chatResponse.getProvider())
                    .model(chatResponse.getModel())
                    .usage(chatResponse.getUsage())
                    .latencyMs((int)(endTime - startTime))
                    .success(chatResponse.isSuccess())
                    .build();

        } catch (Exception e) {
            log.error("RAG query failed", e);
            return RagResponse.builder()
                    .success(false)
                    .error("RAG query failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 检索相关文档
     */
    private List<RetrievalResult> retrieveDocuments(RagRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : DEFAULT_RETRIEVAL_TOP_K;
        double minScore = request.getMinSimilarity() != null ? request.getMinSimilarity() : DEFAULT_MIN_SIMILARITY;

        // 将查询向量化
        List<Double> queryVector = request.getQueryVector();
        if (queryVector == null || queryVector.isEmpty()) {
            // 如果没有提供向量，需要调用embedding服务进行向量化
            // 这里简化处理，实际应该调用EmbeddingService
            throw new IllegalArgumentException("Query vector is required for retrieval");
        }

        // 执行向量搜索
        List<VectorStore.SearchResult> searchResults = vectorStore.searchWithFilter(
                request.getCollectionName(),
                queryVector,
                topK * 2, // 检索更多结果用于过滤
                minScore,
                request.getMetadataFilter()
        );

        // 转换为RetrievalResult
        return searchResults.stream()
                .map(result -> RetrievalResult.builder()
                        .id(result.getId())
                        .content((String) result.getMetadata().getOrDefault("content", ""))
                        .metadata(result.getMetadata())
                        .score(result.getScore())
                        .vector(result.getVector())
                        .build())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<RetrievalResult> retrievals, int maxLength) {
        StringBuilder context = new StringBuilder();
        int currentLength = 0;

        for (RetrievalResult retrieval : retrievals) {
            String content = retrieval.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }

            // 检查是否超出最大长度
            if (currentLength + content.length() > maxLength) {
                // 只添加剩余空间能容纳的部分
                int remaining = maxLength - currentLength;
                if (remaining > 100) { // 至少保留100个字符
                    context.append(content, 0, remaining);
                    context.append("\n\n");
                }
                break;
            }

            context.append(content);
            context.append("\n\n");
            currentLength += content.length() + 2;
        }

        return context.toString().trim();
    }

    /**
     * 构建提示词
     * 优先使用提示词模板管理器中的模板，如未配置则使用默认模板
     */
    private String buildPrompt(String query, String context, String customSystemPrompt) {
        // 尝试从模板管理器获取RAG问答场景的默认模板
        PromptTemplate template = promptTemplateManager.getDefaultTemplateBySceneType(DEFAULT_RAG_SCENE_TYPE);

        if (template != null && template.isEnabled()) {
            // 使用模板渲染
            Map<String, Object> variables = new HashMap<>();
            variables.put("query", query);
            variables.put("context", context);
            variables.put("systemPrompt", customSystemPrompt != null ? customSystemPrompt :
                    "你是一个有用的助手。请根据提供的上下文信息回答用户的问题。");
            return template.render(variables);
        }

        // 回退到默认提示词构建逻辑
        String systemPrompt = customSystemPrompt != null ? customSystemPrompt :
                "你是一个有用的助手。请根据提供的上下文信息回答用户的问题。" +
                        "如果上下文信息不足以回答问题，请明确说明。";

        return String.format(
                "系统提示：%s\n\n" +
                        "上下文信息：\n%s\n\n" +
                        "用户问题：%s\n\n" +
                        "请根据上下文信息回答问题：",
                systemPrompt, context, query
        );
    }

    /**
     * 调用LLM生成回答
     */
    private ChatResponse generateResponse(String prompt, RagRequest request) {
        // 获取Provider
        AIProvider provider;
        if (request.getProvider() != null) {
            provider = providerFactory.getProvider(request.getProvider());
        } else {
            provider = providerFactory.getProvider("openai");
        }

        if (provider == null) {
            throw new IllegalStateException("No AI provider available");
        }

        // 构建请求
        ChatRequest chatRequest = ChatRequest.builder()
                .model(request.getModel() != null ? request.getModel() : "gpt-3.5-turbo")
                .messages(Collections.singletonList(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .maxTokens(request.getMaxTokens())
                .build();

        return provider.chat(chatRequest);
    }

    // ============ RAG 请求/响应对象 ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagRequest {
        /**
         * 用户查询
         */
        private String query;

        /**
         * 查询向量（可选，如果不提供需要外部向量化）
         */
        private List<Double> queryVector;

        /**
         * 集合名称
         */
        private String collectionName;

        /**
         * 检索结果数量
         */
        private Integer topK;

        /**
         * 最小相似度阈值
         */
        private Double minSimilarity;

        /**
         * 元数据过滤条件
         */
        private Map<String, Object> metadataFilter;

        /**
         * 系统提示词
         */
        private String systemPrompt;

        /**
         * 最大上下文长度
         */
        private Integer maxContextLength;

        /**
         * 使用的AI Provider
         */
        private String provider;

        /**
         * 使用的模型
         */
        private String model;

        /**
         * 温度参数
         */
        private Double temperature;

        /**
         * 最大token数
         */
        private Integer maxTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagResponse {
        /**
         * 生成的回答
         */
        private String answer;

        /**
         * 检索结果
         */
        private List<RetrievalResult> retrievals;

        /**
         * 使用的上下文
         */
        private String context;

        /**
         * 使用的提示词
         */
        private String prompt;

        /**
         * 使用的Provider
         */
        private String provider;

        /**
         * 使用的模型
         */
        private String model;

        /**
         * Token使用情况
         */
        private ChatResponse.Usage usage;

        /**
         * 延迟（毫秒）
         */
        private Integer latencyMs;

        /**
         * 是否成功
         */
        private Boolean success;

        /**
         * 错误信息
         */
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalResult {
        /**
         * 文档ID
         */
        private String id;

        /**
         * 关联文档ID（与 id 等价，用于业务层引用）
         */
        private String documentId;

        /**
         * 分块序号
         */
        private Integer chunkIndex;

        /**
         * 文档标题
         */
        private String documentTitle;

        /**
         * 文档内容
         */
        private String content;

        /**
         * 元数据
         */
        private Map<String, Object> metadata;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 向量
         */
        private List<Double> vector;
    }
}
