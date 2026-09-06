package com.aisaas.common.ai.provider;

import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import reactor.core.publisher.Flux;

/**
 * AI Provider 接口定义
 * 统一不同AI提供商（OpenAI、DeepSeek、Claude等）的调用方式
 */
public interface AIProvider {

    /**
     * 获取Provider名称
     *
     * @return Provider名称，如 "openai", "deepseek", "claude"
     */
    String getProviderName();

    /**
     * 获取Provider支持的模型列表
     *
     * @return 支持的模型ID列表
     */
    String[] getSupportedModels();

    /**
     * 检查是否支持指定模型
     *
     * @param model 模型ID
     * @return 是否支持
     */
    boolean supportsModel(String model);

    /**
     * 同步聊天接口
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天接口（SSE）
     *
     * @param request 聊天请求
     * @return 流式响应
     */
    Flux<ChatResponse> streamChat(ChatRequest request);

    /**
     * 单条文本向量化
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * 批量文本向量化
     *
     * @param request 嵌入请求（包含inputs列表）
     * @return 嵌入响应
     */
    EmbeddingResponse embedBatch(EmbeddingRequest request);

    /**
     * 获取模型Token限制
     *
     * @param model 模型ID
     * @return Token限制（输入+输出的总限制）
     */
    int getModelTokenLimit(String model);

    /**
     * 计算Token数量（估算）
     *
     * @param text 文本内容
     * @return Token数量
     */
    int estimateTokenCount(String text);

    /**
     * 健康检查
     *
     * @return 是否健康
     */
    boolean healthCheck();

    /**
     * 获取模型价格信息
     *
     * @param model 模型ID
     * @return 价格信息（输入价格/输出价格，单位：USD/1K tokens）
     */
    ModelPrice getModelPrice(String model);

    /**
     * 模型价格信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ModelPrice {
        /**
         * 输入价格（USD/1K tokens）
         */
        private double inputPrice;

        /**
         * 输出价格（USD/1K tokens）
         */
        private double outputPrice;

        /**
         * 计算总成本
         *
         * @param inputTokens  输入token数
         * @param outputTokens 输出token数
         * @return 总成本（USD）
         */
        public double calculateCost(int inputTokens, int outputTokens) {
            return (inputTokens / 1000.0) * inputPrice + (outputTokens / 1000.0) * outputPrice;
        }
    }
}
