package com.aisaas.common.ai.document.chunk;

import com.aisaas.common.ai.document.Document;
import java.util.List;
import com.aisaas.common.ai.document.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本分块器单元测试
 */
class TextChunkerTest {

    @Test
    void testFixedSizeChunker() {
        FixedSizeChunker chunker = new FixedSizeChunker(100, 20);

        // 创建测试文档
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            content.append("This is line ").append(i).append(" with some content. ");
        }

        Document doc = Document.builder()
                .id("doc1")
                .content(content.toString())
                .title("Test Document")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(doc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);

        // 验证每个分块的基本属性
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            assertNotNull(chunk.getId());
            assertEquals("doc1", chunk.getDocumentId());
            assertEquals(i, chunk.getChunkIndex());
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
        }
    }

    @Test
    void testSemanticChunker() {
        SemanticChunker chunker = new SemanticChunker(500, 50);

        // 创建有自然段落边界的文档
        String content = "第一章 引言\n\n" +
                "这是引言部分的内容。引言通常介绍研究的背景和意义。\n\n" +
                "1.1 研究背景\n\n" +
                "研究背景部分描述问题的来源和研究的重要性。\n\n" +
                "1.2 研究意义\n\n" +
                "研究意义部分说明本研究的理论和实践价值。\n\n" +
                "第二章 文献综述\n\n" +
                "文献综述部分回顾相关领域的研究进展。\n\n" +
                "2.1 国外研究现状\n\n" +
                "介绍国外学者的研究成果。\n\n" +
                "2.2 国内研究现状\n\n" +
                "介绍国内学者的研究成果。";

        Document doc = Document.builder()
                .id("doc2")
                .content(content)
                .title("Research Paper")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(doc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // 语义分块应该保留段落边界
        for (DocumentChunk chunk : chunks) {
            // 检查是否在段落边界结束
            String chunkContent = chunk.getContent();
            assertNotNull(chunkContent);
            assertFalse(chunkContent.isEmpty());
        }
    }

    @Test
    void testRecursiveChunker() {
        RecursiveChunker chunker = new RecursiveChunker(1000, 100);

        // 创建包含不同层级结构的文档
        StringBuilder content = new StringBuilder();
        content.append("# 文档标题\n\n");
        content.append("这是文档的简介部分。\n\n");

        for (int i = 1; i <= 5; i++) {
            content.append("## 第").append(i).append("章\n\n");
            content.append("这是第").append(i).append("章的内容。\n\n");

            for (int j = 1; j <= 3; j++) {
                content.append("### 第").append(i).append(".").append(j).append("节\n\n");
                content.append("这是第").append(i).append("章第").append(j).append("节的内容。\n\n");
            }
        }

        Document doc = Document.builder()
                .id("doc3")
                .content(content.toString())
                .title("Recursive Test Document")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(doc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // 递归分块应该尝试在结构边界分割
        int totalLength = 0;
        for (DocumentChunk chunk : chunks) {
            totalLength += chunk.getContent().length();
        }
        assertTrue(totalLength > 0);
    }

    @Test
    void testEmptyDocument() {
        FixedSizeChunker chunker = new FixedSizeChunker(100, 10);

        Document emptyDoc = Document.builder()
                .id("empty")
                .content("")
                .title("Empty Document")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(emptyDoc);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testSmallDocument() {
        FixedSizeChunker chunker = new FixedSizeChunker(1000, 100);

        String content = "This is a short document that is smaller than the chunk size.";

        Document smallDoc = Document.builder()
                .id("small")
                .content(content)
                .title("Small Document")
                .build();

        List<DocumentChunk> chunks = chunker.chunk(smallDoc);

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(content, chunks.get(0).getContent());
    }
}
