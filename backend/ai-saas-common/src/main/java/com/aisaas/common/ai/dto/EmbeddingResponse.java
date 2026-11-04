package com.aisaas.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 嵌入向量响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /**
     * 响应ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 模型ID
     */
    private String model;

    /**
     * 嵌入向量列表
     */
    private List<Embedding> data;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 错误信息
     */
    private ErrorInfo error;

    /**
     * 向量维度
     */
    private Integer dimension;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return error == null && data != null && !data.isEmpty();
    }

    /**
     * 获取第一个向量
     */
    public List<Double> getFirstEmbedding() {
        if (data != null && !data.isEmpty() && data.get(0) != null) {
            return data.get(0).getEmbedding();
        }
        return null;
    }

    /**
     * 嵌入向量
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Embedding {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 对象类型
         */
        private String object;

        /**
         * 向量数据
         */
        private List<Double> embedding;
    }

    /**
     * Token使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 提示token数
         */
        private Integer promptTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }

    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;

        /**
         * 错误类型
         */
        private String type;
    }
}
