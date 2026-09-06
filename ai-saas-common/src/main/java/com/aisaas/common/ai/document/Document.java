package com.aisaas.common.ai.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档对象
 * 表示解析后的文档内容和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一ID
     */
    private String id;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档类型 (pdf, docx, txt, md, etc.)
     */
    private String docType;

    /**
     * 文档来源（文件路径或URL）
     */
    private String source;

    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;

    /**
     * 字符数
     */
    private Integer charCount;

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
     * 获取元数据值
     *
     * @param key 元数据键
     * @return 元数据值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 设置元数据值
     *
     * @param key   元数据键
     * @param value 元数据值
     */
    public void setMetadataValue(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
}
