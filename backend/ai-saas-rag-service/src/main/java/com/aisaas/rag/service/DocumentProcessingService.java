package com.aisaas.rag.service;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.DocumentChunk;
import com.aisaas.common.ai.document.chunk.TextChunker;
import com.aisaas.common.ai.document.parser.DocumentParser;
import com.aisaas.common.ai.embedding.EmbeddingService;
import com.aisaas.common.ai.vector.VectorStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档处理服务
 * 提供文档解析、分块、向量化和存储的完整流程
 */
@Slf4j
@Service
public class DocumentProcessingService {

    @Autowired
    private DocumentParser documentParser;

    @Autowired
    private TextChunker textChunker;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 处理文档并存储到向量数据库
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @param collectionName 集合名称
     * @param metadata    元数据
     * @return 处理结果
     */
    public DocumentProcessResult processDocument(
            InputStream inputStream,
            String fileName,
            String collectionName,
            Map<String, Object> metadata) {

        long startTime = System.currentTimeMillis();
        log.info("Starting document processing: {} for collection: {}", fileName, collectionName);

        try {
            // 1. 解析文档
            log.debug("Parsing document: {}", fileName);
            Document document = documentParser.parse(inputStream, fileName, metadata);
            log.info("Document parsed: {} with {} characters", document.getTitle(), document.getCharCount());

            // 2. 分块
            log.debug("Chunking document: {}", document.getId());
            List<DocumentChunk> chunks = textChunker.chunk(document);
            log.info("Document chunked into {} chunks", chunks.size());

            // 3. 检查/创建集合
            if (!vectorStore.collectionExists(collectionName)) {
                int dimension = embeddingService.getDefaultDimension();
                boolean created = vectorStore.createCollection(collectionName, dimension, "cosine");
                if (!created) {
                    throw new RuntimeException("Failed to create collection: " + collectionName);
                }
                log.info("Created collection: {} with dimension: {}", collectionName, dimension);
            }

            // 4. 向量化并存储
            int successCount = 0;
            List<String> failedChunkIds = new ArrayList<>();

            for (DocumentChunk chunk : chunks) {
                try {
                    // 向量化
                    List<Double> vector = embeddingService.embed(chunk.getContent());

                    // 准备元数据
                    Map<String, Object> chunkMetadata = new HashMap<>();
                    if (chunk.getMetadata() != null) {
                        chunkMetadata.putAll(chunk.getMetadata());
                    }
                    chunkMetadata.put("content", chunk.getContent());
                    chunkMetadata.put("documentId", document.getId());
                    chunkMetadata.put("documentTitle", document.getTitle());
                    chunkMetadata.put("chunkIndex", chunk.getChunkIndex());
                    chunkMetadata.put("startPosition", chunk.getStartPosition());
                    chunkMetadata.put("endPosition", chunk.getEndPosition());

                    // 存储到向量数据库
                    boolean success = vectorStore.insert(collectionName, chunk.getId(), vector, chunkMetadata);

                    if (success) {
                        successCount++;
                    } else {
                        failedChunkIds.add(chunk.getId());
                        log.warn("Failed to insert chunk: {} into collection: {}", chunk.getId(), collectionName);
                    }

                } catch (Exception e) {
                    failedChunkIds.add(chunk.getId());
                    log.error("Error processing chunk: {}", chunk.getId(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("Document processing completed: {} chunks successful, {} failed, took {}ms",
                    successCount, failedChunkIds.size(), duration);

            return DocumentProcessResult.builder()
                    .success(true)
                    .documentId(document.getId())
                    .documentTitle(document.getTitle())
                    .totalChunks(chunks.size())
                    .successfulChunks(successCount)
                    .failedChunks(failedChunkIds.size())
                    .failedChunkIds(failedChunkIds)
                    .collectionName(collectionName)
                    .durationMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("Document processing failed for file: {}", fileName, e);

            return DocumentProcessResult.builder()
                    .success(false)
                    .errorMessage("Processing failed: " + e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 处理文档并存储到向量数据库（简化版本）
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @param collectionName 集合名称
     * @return 处理结果
     */
    public DocumentProcessResult processDocument(
            InputStream inputStream,
            String fileName,
            String collectionName) {
        return processDocument(inputStream, fileName, collectionName, null);
    }

    /**
     * 文档处理结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentProcessResult {
        private boolean success;
        private String documentId;
        private String documentTitle;
        private Integer totalChunks;
        private Integer successfulChunks;
        private Integer failedChunks;
        private List<String> failedChunkIds;
        private String collectionName;
        private Long durationMs;
        private String errorMessage;
    }
}
