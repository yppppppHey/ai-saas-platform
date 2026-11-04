package com.aisaas.common.ai.document.chunk;

import com.aisaas.common.ai.document.Document;
import com.aisaas.common.ai.document.DocumentChunk;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 固定大小分块器
 * 按照固定的字符数进行分块，支持重叠
 */
@Slf4j
@Component
public class FixedSizeChunker implements TextChunker {

    @Getter
    private final int chunkSize;

    @Getter
    private final int overlapSize;

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP_SIZE = 200;

    public FixedSizeChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public FixedSizeChunker(int chunkSize, int overlapSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("Overlap size must be non-negative and less than chunk size");
        }
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
    }

    @Override
    public String getStrategyName() {
        return "fixed_size";
    }

    @Override
    public List<DocumentChunk> chunk(Document document) {
        if (document == null || document.getContent() == null) {
            return new ArrayList<>();
        }

        String documentId = document.getId();
        String content = document.getContent();
        Map<String, Object> baseMetadata = document.getMetadata() != null
                ? new java.util.HashMap<>(document.getMetadata())
                : new java.util.HashMap<>();

        baseMetadata.put("documentId", documentId);
        baseMetadata.put("documentTitle", document.getTitle());
        baseMetadata.put("documentSource", document.getSource());

        List<DocumentChunk> chunks = new ArrayList<>();
        List<String> textChunks = chunkText(content);

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);
            int startPos = i * (chunkSize - overlapSize);
            int endPos = Math.min(startPos + chunkContent.length(), content.length());

            DocumentChunk chunk = DocumentChunk.builder()
                    .id(generateChunkId())
                    .documentId(documentId)
                    .content(chunkContent)
                    .chunkIndex(i)
                    .startPosition(startPos)
                    .endPosition(endPos)
                    .createTime(System.currentTimeMillis())
                    .metadata(new java.util.HashMap<>(baseMetadata))
                    .build();

            chunk.calculateCharCount();
            chunk.estimateTokenCount();

            chunks.add(chunk);
        }

        log.debug("Document {} split into {} chunks", documentId, chunks.size());
        return chunks;
    }

    @Override
    public List<String> chunkText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        int step = chunkSize - overlapSize;

        for (int start = 0; start < textLength; start += step) {
            int end = Math.min(start + chunkSize, textLength);
            String chunk = text.substring(start, end);
            chunks.add(chunk);

            // 如果已经到达文本末尾，结束循环
            if (end >= textLength) {
                break;
            }
        }

        return chunks;
    }

    @Override
    public List<DocumentChunk> chunkText(String text, Map<String, Object> metadata) {
        List<String> textChunks = chunkText(text);
        List<DocumentChunk> chunks = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);

            DocumentChunk chunk = DocumentChunk.builder()
                    .id(generateChunkId())
                    .content(chunkContent)
                    .chunkIndex(i)
                    .createTime(System.currentTimeMillis())
                    .metadata(metadata != null ? new java.util.HashMap<>(metadata) : new java.util.HashMap<>())
                    .build();

            chunk.calculateCharCount();
            chunk.estimateTokenCount();

            chunks.add(chunk);
        }

        return chunks;
    }

    private String generateChunkId() {
        return "chunk_" + UUID.randomUUID().toString().replace("-", "");
    }
}
