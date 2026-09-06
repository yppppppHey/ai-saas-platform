package com.aisaas.rag.config;

import com.aisaas.common.ai.document.chunk.TextChunker;
import com.aisaas.common.ai.document.parser.DocumentParser;
import com.aisaas.common.ai.document.parser.PdfDocumentParser;
import com.aisaas.common.ai.document.parser.TextDocumentParser;
import com.aisaas.common.ai.document.parser.WordDocumentParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理配置
 */
@Configuration
public class DocumentProcessingConfig {

    @Bean
    @Primary
    public DocumentParser documentParser() {
        return new CompositeDocumentParser();
    }

    @Bean
    public List<DocumentParser> documentParsers() {
        List<DocumentParser> parsers = new ArrayList<>();
        parsers.add(new PdfDocumentParser());
        parsers.add(new WordDocumentParser());
        parsers.add(new TextDocumentParser());
        return parsers;
    }

    @Bean
    public TextChunker textChunker() {
        return new com.aisaas.common.ai.document.chunk.SemanticChunker();
    }

    /**
     * 组合文档解析器
     * 根据文件类型选择对应的解析器
     */
    public static class CompositeDocumentParser implements DocumentParser {

        private final List<DocumentParser> parsers = new ArrayList<>();

        public CompositeDocumentParser() {
            // 注册默认解析器
            parsers.add(new PdfDocumentParser());
            parsers.add(new WordDocumentParser());
            parsers.add(new TextDocumentParser());
        }

        @Override
        public boolean supports(String fileName) {
            return parsers.stream().anyMatch(p -> p.supports(fileName));
        }

        @Override
        public boolean supportsType(String fileType) {
            return parsers.stream().anyMatch(p -> p.supportsType(fileType));
        }

        @Override
        public String[] getSupportedTypes() {
            return parsers.stream()
                    .flatMap(p -> java.util.Arrays.stream(p.getSupportedTypes()))
                    .distinct()
                    .toArray(String[]::new);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.File file) {
            DocumentParser parser = findParser(file.getName());
            return parser.parse(file);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.File file, java.util.Map<String, Object> metadata) {
            DocumentParser parser = findParser(file.getName());
            return parser.parse(file, metadata);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.InputStream inputStream, String fileName) {
            DocumentParser parser = findParser(fileName);
            return parser.parse(inputStream, fileName);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.InputStream inputStream, String fileName,
                                                              java.util.Map<String, Object> metadata) {
            DocumentParser parser = findParser(fileName);
            return parser.parse(inputStream, fileName, metadata);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(byte[] bytes, String fileName) {
            DocumentParser parser = findParser(fileName);
            return parser.parse(bytes, fileName);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(byte[] bytes, String fileName,
                                                            java.util.Map<String, Object> metadata) {
            DocumentParser parser = findParser(fileName);
            return parser.parse(bytes, fileName, metadata);
        }

        private DocumentParser findParser(String fileName) {
            return parsers.stream()
                    .filter(p -> p.supports(fileName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No parser found for file: " + fileName));
        }
    }
}
