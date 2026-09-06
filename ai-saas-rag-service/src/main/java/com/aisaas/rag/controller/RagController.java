package com.aisaas.rag.controller;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.DocumentChunk;
import com.aisaas.common.ai.document.chunk.TextChunker;
import com.aisaas.common.ai.document.parser.DocumentParser;
import com.aisaas.common.ai.embedding.EmbeddingService;
import com.aisaas.common.ai.rag.RagService;
import com.aisaas.common.ai.vector.VectorStore;
import com.aisaas.common.result.Result;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 控制器
 * 提供文档上传、检索问答等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

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

    /**
     * RAG 问答
     */
    @PostMapping("/query")
    public Result<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        log.info("RAG query: {}", request.getQuery());

        try {
            // 将查询向量化
            List<Double> queryVector = embeddingService.embed(request.getQuery());

            // 构建RAG请求
            RagService.RagRequest ragRequest = RagService.RagRequest.builder()
                    .query(request.getQuery())
                    .queryVector(queryVector)
                    .collectionName(request.getCollectionName())
                    .topK(request.getTopK() != null ? request.getTopK() : 5)
                    .minSimilarity(request.getMinSimilarity() != null ? request.getMinSimilarity() : 0.7)
                    .systemPrompt(request.getSystemPrompt())
                    .provider(request.getProvider())
                    .model(request.getModel())
                    .build();

            // 执行RAG
            RagService.RagResponse ragResponse = ragService.ragQuery(ragRequest);

            // 转换为响应
            RagQueryResponse response = RagQueryResponse.builder()
                    .answer(ragResponse.getAnswer())
                    .retrievals(ragResponse.getRetrievals().stream()
                            .map(r -> RetrievalInfo.builder()
                                    .content(r.getContent())
                                    .score(r.getScore())
                                    .metadata(r.getMetadata())
                                    .build())
                            .collect(Collectors.toList()))
                    .latencyMs(ragResponse.getLatencyMs())
                    .model(ragResponse.getModel())
                    .build();

            return Result.success(response);

        } catch (Exception e) {
            log.error("RAG query failed", e);
            return Result.error("RAG query failed: " + e.getMessage());
        }
    }

    /**
     * 文档上传并索引
     */
    @PostMapping("/documents/upload")
    public Result<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("collectionName") String collectionName,
            @RequestParam(value = "documentId", required = false) String documentId) {
        log.info("Uploading document: {} to collection: {}", file.getOriginalFilename(), collectionName);

        try {
            // 解析文档
            Document document = documentParser.parse(file.getInputStream(), file.getOriginalFilename());

            // 设置文档ID
            if (documentId != null) {
                document.setId(documentId);
            }

            // 分块
            List<DocumentChunk> chunks = textChunker.chunk(document);
            log.info("Document split into {} chunks", chunks.size());

            // 检查集合是否存在，不存在则创建
            if (!vectorStore.collectionExists(collectionName)) {
                int dimension = embeddingService.getDefaultDimension();
                vectorStore.createCollection(collectionName, dimension, "cosine");
                log.info("Created collection: {} with dimension: {}", collectionName, dimension);
            }

            // 向量化并存储
            int successCount = 0;
            for (DocumentChunk chunk : chunks) {
                List<Double> vector = embeddingService.embed(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.putAll(chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
                metadata.put("content", chunk.getContent());
                metadata.put("documentId", document.getId());
                metadata.put("documentTitle", document.getTitle());
                metadata.put("chunkIndex", chunk.getChunkIndex());

                boolean success = vectorStore.insert(collectionName, chunk.getId(), vector, metadata);
                if (success) {
                    successCount++;
                }
            }

            log.info("Successfully indexed {}/{} chunks", successCount, chunks.size());

            return Result.success(DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .documentTitle(document.getTitle())
                    .chunkCount(chunks.size())
                    .indexedCount(successCount)
                    .collectionName(collectionName)
                    .build());

        } catch (IOException e) {
            log.error("Failed to read file", e);
            return Result.error("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process document", e);
            return Result.error("Failed to process document: " + e.getMessage());
        }
    }

    /**
     * 创建集合
     */
    @PostMapping("/collections")
    public Result<Boolean> createCollection(@RequestBody CreateCollectionRequest request) {
        log.info("Creating collection: {}", request.getCollectionName());

        try {
            int dimension = request.getDimension() != null ? request.getDimension()
                    : embeddingService.getDefaultDimension();
            String metricType = request.getMetricType() != null ? request.getMetricType() : "cosine";

            boolean success = vectorStore.createCollection(request.getCollectionName(), dimension, metricType);

            if (success) {
                return Result.success(true);
            } else {
                return Result.error("Collection may already exist");
            }

        } catch (Exception e) {
            log.error("Failed to create collection", e);
            return Result.error("Failed to create collection: " + e.getMessage());
        }
    }

    /**
     * 删除集合
     */
    @DeleteMapping("/collections/{collectionName}")
    public Result<Boolean> deleteCollection(@PathVariable String collectionName) {
        log.info("Deleting collection: {}", collectionName);

        try {
            boolean success = vectorStore.deleteCollection(collectionName);

            if (success) {
                return Result.success(true);
            } else {
                return Result.error("Collection does not exist");
            }

        } catch (Exception e) {
            log.error("Failed to delete collection", e);
            return Result.error("Failed to delete collection: " + e.getMessage());
        }
    }

    // ============ 请求/响应对象 ============

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagQueryRequest {
        private String query;
        private String collectionName;
        private Integer topK;
        private Double minSimilarity;
        private String systemPrompt;
        private String provider;
        private String model;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagQueryResponse {
        private String answer;
        private List<RetrievalInfo> retrievals;
        private Integer latencyMs;
        private String model;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalInfo {
        private String content;
        private Double score;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentUploadResponse {
        private String documentId;
        private String documentTitle;
        private Integer chunkCount;
        private Integer indexedCount;
        private String collectionName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCollectionRequest {
        private String collectionName;
        private Integer dimension;
        private String metricType;
    }
}
