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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义分块器
 * 基于语义边界（段落、句子）进行分块，优先保持语义完整性
 */
@Slf4j
@Component
public class SemanticChunker implements TextChunker {

    @Getter
    private final int chunkSize;

    @Getter
    private final int overlapSize;

    // 句子分隔符正则表达式（中英文）
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "[^。？！;.!?]+[。？！;.!?]"
    );

    // 段落分隔符
    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP_SIZE = 100;

    public SemanticChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public SemanticChunker(int chunkSize, int overlapSize) {
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
        return "semantic";
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
        baseMetadata.put("chunkerStrategy", getStrategyName());

        List<DocumentChunk> chunks = new ArrayList<>();
        List<String> semanticUnits = splitIntoSemanticUnits(content);

        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;
        int chunkIndex = 0;
        int startPosition = 0;

        for (String unit : semanticUnits) {
            int unitSize = unit.length();

            // 如果当前单元加上后超过chunkSize，先保存当前chunk
            if (currentSize + unitSize > chunkSize && currentSize > 0) {
                // 保存当前chunk
                String chunkContent = currentChunk.toString().trim();
                if (!chunkContent.isEmpty()) {
                    DocumentChunk chunk = createChunk(chunkContent, documentId, chunkIndex,
                            startPosition, startPosition + chunkContent.length(), baseMetadata);
                    chunks.add(chunk);
                    chunkIndex++;
                }

                // 处理重叠：保留最后overlapSize个字符
                String overlapText = currentChunk.substring(
                        Math.max(0, currentChunk.length() - overlapSize));
                currentChunk = new StringBuilder(overlapText);
                currentSize = overlapText.length();
                startPosition = startPosition + chunkContent.length() - overlapText.length();
            }

            // 添加当前单元
            currentChunk.append(unit);
            currentSize += unitSize;
        }

        // 处理最后一个chunk
        String lastChunkContent = currentChunk.toString().trim();
        if (!lastChunkContent.isEmpty()) {
            DocumentChunk lastChunk = createChunk(lastChunkContent, documentId, chunkIndex,
                    startPosition, startPosition + lastChunkContent.length(), baseMetadata);
            chunks.add(lastChunk);
        }

        log.debug("Document {} split into {} chunks using semantic strategy", documentId, chunks.size());
        return chunks;
    }

    /**
     * 将文本分割成语义单元（段落 -> 句子 -> 单词）
     */
    private List<String> splitIntoSemanticUnits(String text) {
        List<String> units = new ArrayList<>();

        // 首先按段落分割
        String[] paragraphs = text.split(PARAGRAPH_SEPARATOR);
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // 如果段落长度超过chunkSize的一半，按句子分割
            if (paragraph.length() > chunkSize / 2) {
                List<String> sentences = splitIntoSentences(paragraph);
                units.addAll(sentences);
            } else {
                units.add(paragraph + PARAGRAPH_SEPARATOR);
            }
        }

        return units;
    }

    /**
     * 将文本分割成句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // 添加当前句子
            sentences.add(matcher.group());
            lastEnd = matcher.end();
        }

        // 处理最后一个句子（可能没有标点符号结尾）
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }

        return sentences;
    }

    @Override
    public List<String> chunkText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        List<String> semanticUnits = splitIntoSemanticUnits(text);

        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (String unit : semanticUnits) {
            int unitSize = unit.length();

            // 如果当前单元加上后超过chunkSize，先保存当前chunk
            if (currentSize + unitSize > chunkSize && currentSize > 0) {
                String chunkContent = currentChunk.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(chunkContent);
                }

                // 处理重叠
                String overlapText = currentChunk.substring(
                        Math.max(0, currentChunk.length() - overlapSize));
                currentChunk = new StringBuilder(overlapText);
                currentSize = overlapText.length();
            }

            // 添加当前单元
            currentChunk.append(unit);
            currentSize += unitSize;
        }

        // 处理最后一个chunk
        String lastChunkContent = currentChunk.toString().trim();
        if (!lastChunkContent.isEmpty()) {
            chunks.add(lastChunkContent);
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

    private DocumentChunk createChunk(String content, String documentId, int chunkIndex,
                                      int startPosition, int endPosition, Map<String, Object> metadata) {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(generateChunkId())
                .documentId(documentId)
                .content(content)
                .chunkIndex(chunkIndex)
                .startPosition(startPosition)
                .endPosition(endPosition)
                .createTime(System.currentTimeMillis())
                .metadata(new java.util.HashMap<>(metadata))
                .build();

        chunk.calculateCharCount();
        chunk.estimateTokenCount();

        return chunk;
    }

    private String generateChunkId() {
        return "chunk_" + UUID.randomUUID().toString().replace("-", "");
    }
}
