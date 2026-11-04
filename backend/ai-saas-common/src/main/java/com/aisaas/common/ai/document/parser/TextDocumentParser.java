package com.aisaas.common.ai.document.parser;

import com.aisaas.common.ai.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 文本文档解析器
 * 支持 .txt, .md, .json, .xml, .csv 等纯文本格式
 */
@Slf4j
@Component
public class TextDocumentParser extends AbstractDocumentParser {

    private static final String[] SUPPORTED_TYPES = {
            "txt", "text", "md", "markdown",
            "json", "xml", "yaml", "yml",
            "csv", "tsv", "properties",
            "java", "py", "js", "html", "css", "sql"
    };

    @Override
    public boolean supportsType(String fileType) {
        if (fileType == null) {
            return false;
        }
        String lowerType = fileType.toLowerCase();
        for (String type : SUPPORTED_TYPES) {
            if (type.equals(lowerType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return SUPPORTED_TYPES.clone();
    }

    @Override
    public Document parse(InputStream inputStream, String fileName, Map<String, Object> metadata) {
        return doParse(inputStream, fileName, metadata);
    }

    @Override
    protected Document doParse(InputStream inputStream, String fileName, Map<String, Object> metadata) {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String fileType = extractFileType(fileName);
            Document document = createBaseDocument(content.toString().trim(), fileName, fileType, metadata);

            // 针对Markdown特殊处理
            if ("md".equals(fileType) || "markdown".equals(fileType)) {
                document.setTitle(extractMarkdownTitle(content.toString()));
            }

            log.debug("Parsed text document: {} with {} characters", fileName, document.getCharCount());
            return document;

        } catch (IOException e) {
            log.error("Failed to parse text file: {}", fileName, e);
            throw new RuntimeException("Failed to parse text file: " + fileName, e);
        }
    }

    /**
     * 提取Markdown标题（第一个#开头的行）
     */
    private String extractMarkdownTitle(String content) {
        if (content == null) {
            return null;
        }
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ") && trimmed.length() > 2) {
                return trimmed.substring(2).trim();
            }
        }
        return null;
    }
}
