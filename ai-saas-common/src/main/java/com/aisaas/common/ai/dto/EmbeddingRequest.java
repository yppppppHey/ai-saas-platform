package com.aisaas.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 嵌入向量请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 输入文本（单条）
     */
    private String input;

    /**
     * 输入文本列表（批量）
     */
    private List<String> inputs;

    /**
     * 编码格式
     */
    private String encodingFormat;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 维度（部分模型支持）
     */
    private Integer dimensions;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 是否为批量请求
     */
    public boolean isBatch() {
        return inputs != null && !inputs.isEmpty();
    }
}
