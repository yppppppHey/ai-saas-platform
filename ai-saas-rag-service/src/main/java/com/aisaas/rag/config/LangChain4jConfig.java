package com.aisaas.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置类
 * 配置LangChain4j相关的模型和组件
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Value("${openai.timeout-seconds:60}")
    private int openaiTimeoutSeconds;

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6334}")
    private int qdrantPort;

    @Value("${qdrant.api-key:}")
    private String qdrantApiKey;

    @Value("${qdrant.use-tls:false}")
    private boolean qdrantUseTls;

    /**
     * 创建 OpenAI Chat 模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, chat language model will not be available");
            return null;
        }

        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .modelName(openaiModel)
                .timeout(Duration.ofSeconds(openaiTimeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建 OpenAI Embedding 模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, embedding model will not be available");
            return null;
        }

        return OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .modelName("text-embedding-3-small")
                .timeout(Duration.ofSeconds(30))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /**
     * 创建 Qdrant Embedding Store
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        try {
            QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                    .host(qdrantHost)
                    .port(qdrantPort)
                    .useTls(qdrantUseTls);

            if (qdrantApiKey != null && !qdrantApiKey.isEmpty()) {
                builder.apiKey(qdrantApiKey);
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to create Qdrant embedding store", e);
            return null;
        }
    }

    /**
     * 创建检索增强器
     */
    @Bean
    public RetrievalAugmentor retrievalAugmentor(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        if (embeddingStore == null || embeddingModel == null) {
            log.warn("Embedding store or model not available, retrieval augmentor will not be created");
            return null;
        }

        // 创建内容检索器
        ContentRetriever contentRetriever = dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();

        // 创建查询压缩转换器
        CompressingQueryTransformer queryTransformer = new CompressingQueryTransformer(chatLanguageModel());

        // 创建检索增强器
        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer)
                .contentRetriever(contentRetriever)
                .build();
    }
}
