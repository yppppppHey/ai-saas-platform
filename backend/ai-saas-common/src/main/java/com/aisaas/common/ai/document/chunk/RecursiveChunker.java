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
import java.util.regex.Pattern;

/**
 * 递归分块器
 * 按照分隔符层级递归分块，优先保持段落/句子完整性
 * 分隔符优先级：段落(\\n\\n) > 句子(\\n) > 单词(\\s) > 字符
 */
@Slf4j
@Component
public class RecursiveChunker implements TextChunker {

    @Getter
    private final int chunkSize;

    @Getter
    private final int overlapSize;

    // 分隔符优先级列表（从高到低）
    private final String[] separators;

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_OVERLAP_SIZE = 200;
    private static final String[] DEFAULT_SEPARATORS = {
            "\\n\\n",    // 段落
            "\\n",      // 换行
            "。",        // 中文句号
            "；",        // 中文分号
            "！",        // 中文感叹号
            "？",        // 中文问号
            ",",        // 英文逗号
            ".",        // 英文句号
            ";",        // 英文分号
            "!",        // 英文感叹号
            "?",        // 英文问号
            " "         // 空格
    };

    public RecursiveChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE, DEFAULT_SEPARATORS);
    }

    public RecursiveChunker(int chunkSize, int overlapSize) {
        this(chunkSize, overlapSize, DEFAULT_SEPARATORS);
    }

    public RecursiveChunker(int chunkSize, int overlapSize, String[] separators) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (overlapSize < 0 || overlapSize >= chunkSize) {
            throw new IllegalArgumentException("Overlap size must be non-negative and less than chunk size");
        }
        if (separators == null || separators.length == 0) {
            throw new IllegalArgumentException("Separators cannot be empty");
        }
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
        this.separators = separators.clone();
    }

    @Override
    public String getStrategyName() {
        return "recursive";
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
        List<String> textChunks = chunkText(content);

        int currentPosition = 0;
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);
            int startPos = currentPosition;
            int endPos = startPos + chunkContent.length();

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

            // 更新位置（考虑重叠）
            currentPosition = endPos - overlapSize;
            if (currentPosition < 0) currentPosition = 0;
        }

        log.debug("Document {} split into {} chunks using recursive strategy", documentId, chunks.size());
        return chunks;
    }

    @Override
    public List<String> chunkText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();

        // 如果文本长度小于chunkSize，直接返回
        if (textLength <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        // 使用递归分隔符进行分块
        int currentPos = 0;
        while (currentPos < textLength) {
            int endPos = Math.min(currentPos + chunkSize, textLength);

            // 如果不是最后一块，尝试找到最佳的分割点
            if (endPos < textLength) {
                int bestSplit = findBestSplitPoint(text, currentPos, endPos);
                if (bestSplit > currentPos) {
                    endPos = bestSplit;
                }
            }

            String chunk = text.substring(currentPos, endPos);
            chunks.add(chunk);

            // 移动位置（考虑重叠）
            currentPos = endPos - overlapSize;
            if (currentPos <= 0 || currentPos >= textLength) {
                currentPos = endPos;
            }
        }

        return chunks;
    }

    @Override
    public List<DocumentChunk> chunkText(String text, Map<String, Object> metadata) {
        List<String> textChunks = chunkText(text);
        List<DocumentChunk> chunks = new ArrayList<>();

        int currentPosition = 0;
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkContent = textChunks.get(i);
            int startPos = currentPosition;
            int endPos = startPos + chunkContent.length();

            DocumentChunk chunk = DocumentChunk.builder()
                    .id(generateChunkId())
                    .content(chunkContent)
                    .chunkIndex(i)
                    .startPosition(startPos)
                    .endPosition(endPos)
                    .createTime(System.currentTimeMillis())
                    .metadata(metadata != null ? new java.util.HashMap<>(metadata) : new java.util.HashMap<>())
                    .build();

            chunk.calculateCharCount();
            chunk.estimateTokenCount();

            chunks.add(chunk);

            // 更新位置
            currentPosition = endPos - overlapSize;
            if (currentPosition < 0) currentPosition = 0;
        }

        return chunks;
    }

    /**
     * 查找最佳分割点
     * 按照分隔符优先级从高到低查找
     */
    private int findBestSplitPoint(String text, int startPos, int endPos) {
        // 按照分隔符优先级从高到低查找
        for (String separator : separators) {
            // 转义特殊正则字符
            String escapedSep = separator.replace("\\", "\\\\")
                    .replace(".", "\\.")
                    .replace("*", "\\*")
                    .replace("+", "\\+")
                    .replace("?", "\\?")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("{", "\\{")
                    .replace("}", "\\}")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("|", "\\|");

            // 从后往前查找分隔符
            for (int i = endPos; i > startPos; i--) {
                // 检查是否在分隔符位置
                if (matchesSeparator(text, i, separator)) {
                    return i;
                }
            }
        }

        // 如果没有找到合适的分割点，直接返回endPos
        return endPos;
    }

    /**
     * 检查指定位置是否匹配分隔符
     */
    private boolean matchesSeparator(String text, int position, String separator) {
        if (position < separator.length()) {
            return false;
        }
        String substr = text.substring(position - separator.length(), position);
        return substr.equals(separator);
    }

    private String generateChunkId() {
        return "chunk_" + UUID.randomUUID().toString().replace("-", "");
    }
}
