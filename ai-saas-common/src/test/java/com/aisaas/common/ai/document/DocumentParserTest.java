package com.aisaas.common.ai.document;

import com.aisaas.common.ai.document.parser.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档解析器单元测试
 */
class DocumentParserTest {

    @Test
    void testTextDocumentParser() {
        TextDocumentParser parser = new TextDocumentParser();

        // 测试支持的类型
        assertTrue(parser.supportsType("txt"));
        assertTrue(parser.supportsType("md"));
        assertTrue(parser.supportsType("json"));
        assertFalse(parser.supportsType("pdf"));

        // 测试文件解析
        String content = "This is a test document.\nIt has multiple lines.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        Document doc = parser.parse(bytes, "test.txt");
        assertNotNull(doc);
        assertEquals(content, doc.getContent());
        assertEquals("test.txt", doc.getSource());
        assertEquals("txt", doc.getDocType());
        assertNotNull(doc.getId());
        assertTrue(doc.getCharCount() > 0);
    }

    @Test
    void testMarkdownDocumentParser() {
        TextDocumentParser parser = new TextDocumentParser();

        String markdownContent = "# Document Title\n\nThis is the content.\n\n## Section 1\n\nMore content here.";
        byte[] bytes = markdownContent.getBytes(StandardCharsets.UTF_8);

        Document doc = parser.parse(bytes, "test.md");
        assertNotNull(doc);
        assertEquals("Document Title", doc.getTitle()); // 应该提取Markdown标题
    }

    @Test
    void testPdfDocumentParserSupports() {
        PdfDocumentParser parser = new PdfDocumentParser();

        assertTrue(parser.supportsType("pdf"));
        assertTrue(parser.supports("document.pdf"));
        assertFalse(parser.supportsType("txt"));
    }

    @Test
    void testWordDocumentParserSupports() {
        WordDocumentParser parser = new WordDocumentParser();

        assertTrue(parser.supportsType("docx"));
        assertTrue(parser.supportsType("doc"));
        assertTrue(parser.supports("document.docx"));
        assertTrue(parser.supports("document.doc"));
        assertFalse(parser.supportsType("txt"));
    }

    @Test
    void testDocumentMetadata() {
        TextDocumentParser parser = new TextDocumentParser();

        String content = "Test content";
        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("author", "Test Author");
        customMetadata.put("category", "Test");

        Document doc = parser.parse(
                content.getBytes(StandardCharsets.UTF_8),
                "test.txt",
                customMetadata
        );

        assertNotNull(doc.getMetadata());
        assertEquals("Test Author", doc.getMetadataValue("author"));
        assertEquals("Test", doc.getMetadataValue("category"));
        assertNotNull(doc.getMetadataValue("parser")); // 自动添加的元数据
        assertNotNull(doc.getMetadataValue("parseTime"));
    }

    @Test
    void testEmptyDocument() {
        TextDocumentParser parser = new TextDocumentParser();

        Document doc = parser.parse("".getBytes(StandardCharsets.UTF_8), "empty.txt");

        assertNotNull(doc);
        assertEquals("", doc.getContent());
        assertEquals(0, doc.getCharCount());
    }
}
