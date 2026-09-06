package com.aisaas.common.ai.document.chunk;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.DocumentChunk;

import java.util.List;

/**
 * 文本分块器接口
 * 定义文档分块的标准接口
 */
public interface TextChunker {

    /**
     * 获取分块策略名称
     *
     * @return 分块策略名称
     */
    String getStrategyName();

    /**
     * 获取分块大小
     *
     * @return 分块大小（字符数或token数）
     */
    int getChunkSize();

    /**
     * 获取重叠大小
     *
     * @return 重叠大小（字符数或token数）
     */
    int getOverlapSize();

    /**
     * 对文档进行分块
     *
     * @param document 文档对象
     * @return 分块列表
     */
    List<DocumentChunk> chunk(Document document);

    /**
     * 对文本进行分块
     *
     * @param text 文本内容
     * @return 分块列表
     */
    List<String> chunkText(String text);

    /**
     * 对文本进行分块（带元数据）
     *
     * @param text     文本内容
     * @param metadata 元数据
     * @return 分块列表
     */
    List<DocumentChunk> chunkText(String text, java.util.Map<String, Object> metadata);
}
