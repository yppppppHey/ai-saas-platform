package com.aisaas.common.ai.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档分块对象
 * 表示文档分块后的片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 分块唯一ID
     */
    private String id;

    /**
     * 所属文档ID
     */
    private String documentId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块索引（在文档中的顺序）
     */
    private Integer chunkIndex;

    /**
     * 开始位置（字符索引）
     */
    private Integer startPosition;

    /**
     * 结束位置（字符索引）
     */
    private Integer endPosition;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * Token数（估算）
     */
    private Integer tokenCount;

    /**
     * 分块元数据
     */
    private Map<String, Object> metadata;

    /**
     * 向量（存储向量化后的结果）
     */
    private java.util.List<Double> vector;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 计算字符数
     */
    public void calculateCharCount() {
        this.charCount = content != null ? content.length() : 0;
    }

    /**
     * 估算Token数（简单估算：中文字符 + 英文单词数）
     */
    public void estimateTokenCount() {
        if (content == null) {
            this.tokenCount = 0;
            return;
        }
        // 简单估算：中文字符算作1个token，英文单词按空格分隔
        int chineseChars = 0;
        int englishWords = 0;
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.matches(".*[\\u4e00-\\u9fa5].*")) {
                chineseChars += word.length();
            } else {
                englishWords++;
            }
        }
        this.tokenCount = chineseChars + englishWords;
    }

    /**
     * 获取元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 设置元数据值
     */
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
}
