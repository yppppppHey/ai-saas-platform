package com.aisaas.common.ai.embedding;

import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.factory.AIProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量化服务
 * 封装文本向量化的业务逻辑
 */
@Slf4j
@Service
public class EmbeddingService {

    @Autowired
    private AIProviderFactory providerFactory;

    // 默认向量化模型
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";
    // 默认向量维度
    private static final int DEFAULT_DIMENSION = 1536;
    // 批量处理大小
    private static final int BATCH_SIZE = 100;

    /**
     * 单条文本向量化
     *
     * @param text 输入文本
     * @return 向量
     */
    public List<Double> embed(String text) {
        return embed(text, DEFAULT_EMBEDDING_MODEL);
    }

    /**
     * 单条文本向量化（指定模型）
     *
     * @param text      输入文本
     * @param modelName 模型名称
     * @return 向量
     */
    public List<Double> embed(String text, String modelName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(modelName)
                .input(text)
                .build();

        EmbeddingResponse response = executeEmbed(request);
        return response.getFirstEmbedding();
    }

    /**
     * 批量文本向量化
     *
     * @param texts 输入文本列表
     * @return 向量列表
     */
    public List<List<Double>> embedBatch(List<String> texts) {
        return embedBatch(texts, DEFAULT_EMBEDDING_MODEL);
    }

    /**
     * 批量文本向量化（指定模型）
     *
     * @param texts     输入文本列表
     * @param modelName 模型名称
     * @return 向量列表
     */
    public List<List<Double>> embedBatch(List<String> texts, String modelName) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // 如果文本数量超过批量限制，分批处理
        if (texts.size() > BATCH_SIZE) {
            List<List<Double>> allEmbeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
                allEmbeddings.addAll(embedBatch(batch, modelName));
            }
            return allEmbeddings;
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(modelName)
                .inputs(texts)
                .build();

        EmbeddingResponse response = executeEmbed(request);

        return response.getData().stream()
                .map(EmbeddingResponse.Embedding::getEmbedding)
                .collect(Collectors.toList());
    }

    /**
     * 执行向量化请求
     */
    private EmbeddingResponse executeEmbed(EmbeddingRequest request) {
        // 查找支持该模型的Provider
        AIProvider provider = providerFactory.getProviderByModel(request.getModel());

        if (provider == null) {
            // 如果没有找到，使用OpenAI作为默认Provider
            provider = providerFactory.getProvider("openai");
            if (provider == null) {
                throw new IllegalStateException("No AI provider available for embedding");
            }
        }

        log.debug("Using provider {} for embedding with model {}",
                provider.getProviderName(), request.getModel());

        if (request.isBatch()) {
            return provider.embedBatch(request);
        } else {
            return provider.embed(request);
        }
    }

    /**
     * 计算向量相似度（余弦相似度）
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度（-1 到 1）
     */
    public double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must be non-null and have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 获取默认向量化模型
     */
    public String getDefaultEmbeddingModel() {
        return DEFAULT_EMBEDDING_MODEL;
    }

    /**
     * 获取默认向量维度
     */
    public int getDefaultDimension() {
        return DEFAULT_DIMENSION;
    }

    /**
     * 模型价格配置内部类
     */
    private static class ModelPriceConfig {
        final double inputPrice;
        final double outputPrice;

        ModelPriceConfig(double inputPrice, double outputPrice) {
            this.inputPrice = inputPrice;
            this.outputPrice = outputPrice;
        }
    }
}
