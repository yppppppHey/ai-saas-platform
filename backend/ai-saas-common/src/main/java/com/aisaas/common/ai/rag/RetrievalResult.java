package com.aisaas.common.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 检索结果
 * 表示从向量数据库中检索到的文档片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /**
     * 结果唯一ID
     */
    private String id;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 相似度分数 (0-1)
     */
    private Double score;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 向量（可选）
     */
    private List<Double> vector;

    /**
     * 所属文档ID
     */
    private String documentId;

    /**
     * 文档标题
     */
    private String documentTitle;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 获取元数据值
     *
     * @param key 元数据键
     * @return 元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 获取字符串类型的元数据值
     *
     * @param key 元数据键
     * @return 字符串值
     */
    public String getMetadataString(String key) {
        Object value = getMetadataValue(key);
        return value != null ? value.toString() : null;
    }
}
