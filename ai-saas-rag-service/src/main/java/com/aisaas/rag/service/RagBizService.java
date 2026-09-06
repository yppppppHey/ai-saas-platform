package com.aisaas.rag.service;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.DocumentChunk;
import com.aisaas.common.ai.document.chunk.TextChunker;
import com.aisaas.common.ai.document.parser.DocumentParser;
import com.aisaas.common.ai.embedding.EmbeddingService;
import com.aisaas.common.ai.rag.RagService;
import com.aisaas.common.ai.rag.RetrievalResult;
import com.aisaas.common.ai.vector.VectorStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 业务服务
 * 封装 RAG 相关的业务逻辑
 */
@Slf4j
@Service
public class RagBizService {

    @Autowired
    private RagService ragService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentParser documentParser;

    @Autowired
    private TextChunker textChunker;

    // 默认分块大小
    private static final int DEFAULT_CHUNK_SIZE = 1000;
    // 默认重叠大小
    private static final int DEFAULT_OVERLAP_SIZE = 100;

    /**
     * 处理文档上传并索引
     */
    public DocumentProcessResult processDocument(MultipartFile file, String collectionName, String documentId) {
        log.info("Processing document: {} for collection: {}", file.getOriginalFilename(), collectionName);

        try {
            // 解析文档
            Document document = documentParser.parse(file.getInputStream(), file.getOriginalFilename());

            if (documentId != null) {
                document.setId(documentId);
            }

            // 分块
            List<DocumentChunk> chunks = textChunker.chunk(document);
            log.info("Document split into {} chunks", chunks.size());

            // 确保集合存在
            ensureCollectionExists(collectionName);

            // 向量化并索引
            int successCount = indexChunks(collectionName, document, chunks);

            log.info("Successfully indexed {}/{} chunks", successCount, chunks.size());

            return DocumentProcessResult.builder()
                    .success(true)
                    .documentId(document.getId())
                    .documentTitle(document.getTitle())
                    .chunkCount(chunks.size())
                    .indexedCount(successCount)
                    .collectionName(collectionName)
                    .build();

        } catch (IOException e) {
            log.error("Failed to read file", e);
            return DocumentProcessResult.builder()
                    .success(false)
                    .errorMessage("Failed to read file: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Failed to process document", e);
            return DocumentProcessResult.builder()
                    .success(false)
                    .errorMessage("Failed to process document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 执行RAG查询
     */
    public RagQueryResult query(RagQueryParam param) {
        log.info("RAG query: {} in collection: {}", param.getQuery(), param.getCollectionName());

        long startTime = System.currentTimeMillis();

        try {
            // 向量化查询
            List<Double> queryVector = embeddingService.embed(param.getQuery());

            // 构建RAG请求
            RagService.RagRequest ragRequest = RagService.RagRequest.builder()
                    .query(param.getQuery())
                    .queryVector(queryVector)
                    .collectionName(param.getCollectionName())
                    .topK(param.getTopK() != null ? param.getTopK() : 5)
                    .minSimilarity(param.getMinSimilarity() != null ? param.getMinSimilarity() : 0.7)
                    .systemPrompt(param.getSystemPrompt())
                    .provider(param.getProvider())
                    .model(param.getModel())
                    .temperature(param.getTemperature())
                    .build();

            // 执行RAG
            RagService.RagResponse ragResponse = ragService.ragQuery(ragRequest);

            long endTime = System.currentTimeMillis();

            // 转换为检索信息
            List<RetrievalInfo> retrievals = ragResponse.getRetrievals().stream()
                    .map(r -> RetrievalInfo.builder()
                            .id(r.getId())
                            .content(r.getContent())
                            .score(r.getScore())
                            .metadata(r.getMetadata())
                            .build())
                    .collect(Collectors.toList());

            return RagQueryResult.builder()
                    .success(ragResponse.isSuccess())
                    .answer(ragResponse.getAnswer())
                    .retrievals(retrievals)
                    .latencyMs((int)(endTime - startTime))
                    .model(ragResponse.getModel())
                    .usage(ragResponse.getUsage())
                    .build();

        } catch (Exception e) {
            log.error("RAG query failed", e);
            return RagQueryResult.builder()
                    .success(false)
                    .errorMessage("RAG query failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 检索（不生成回答）
     */
    public List<RetrievalInfo> retrieve(String query, String collectionName, int topK, double minScore) {
        try {
            // 向量化查询
            List<Double> queryVector = embeddingService.embed(query);

            // 执行向量搜索
            List<VectorStore.SearchResult> results = vectorStore.searchWithFilter(
                    collectionName, queryVector, topK, minScore, null);

            // 转换为检索信息
            return results.stream()
                    .map(r -> RetrievalInfo.builder()
                            .id(r.getId())
                            .content((String) r.getMetadata().getOrDefault("content", ""))
                            .score(r.getScore())
                            .metadata(r.getMetadata())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Retrieval failed", e);
            return new ArrayList<>();
        }
    }

    // ============ 私有方法 ============

    private void ensureCollectionExists(String collectionName) {
        if (!vectorStore.collectionExists(collectionName)) {
            int dimension = embeddingService.getDefaultDimension();
            vectorStore.createCollection(collectionName, dimension, "cosine");
            log.info("Created collection: {} with dimension: {}", collectionName, dimension);
        }
    }

    private int indexChunks(String collectionName, Document document, List<DocumentChunk> chunks) {
        int successCount = 0;

        for (DocumentChunk chunk : chunks) {
            try {
                List<Double> vector = embeddingService.embed(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                if (chunk.getMetadata() != null) {
                    metadata.putAll(chunk.getMetadata());
                }
                metadata.put("content", chunk.getContent());
                metadata.put("documentId", document.getId());
                metadata.put("documentTitle", document.getTitle());
                metadata.put("chunkIndex", chunk.getChunkIndex());
                metadata.put("docType", document.getDocType());

                boolean success = vectorStore.insert(collectionName, chunk.getId(), vector, metadata);
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Failed to index chunk: {}", chunk.getId(), e);
            }
        }

        return successCount;
    }

    // ============ DTO ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentProcessResult {
        private boolean success;
        private String documentId;
        private String documentTitle;
        private Integer chunkCount;
        private Integer indexedCount;
        private String collectionName;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagQueryParam {
        private String query;
        private String collectionName;
        private Integer topK;
        private Double minSimilarity;
        private String systemPrompt;
        private String provider;
        private String model;
        private Double temperature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagQueryResult {
        private boolean success;
        private String answer;
        private List<RetrievalInfo> retrievals;
        private Integer latencyMs;
        private String model;
        private com.aisaas.common.ai.dto.ChatResponse.Usage usage;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalInfo {
        private String id;
        private String content;
        private Double score;
        private java.util.Map<String, Object> metadata;
    }
}
