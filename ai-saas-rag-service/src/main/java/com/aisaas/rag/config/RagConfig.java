package com.aisaas.rag.config;

import com.aisaas.common.ai.document.chunk.FixedSizeChunker;
import com.aisaas.common.ai.document.chunk.RecursiveChunker;
import com.aisaas.common.ai.document.chunk.SemanticChunker;
import com.aisaas.common.ai.document.chunk.TextChunker;
import com.aisaas.common.ai.document.parser.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * RAG 配置类
 * 配置文档解析器和分块器
 */
@Configuration
public class RagConfig {

    /**
     * 配置文档解析器
     * 组合多个解析器提供统一的文档解析能力
     */
    @Bean
    public DocumentParser documentParser() {
        return new CompositeDocumentParser(Arrays.asList(
                new PdfDocumentParser(),
                new WordDocumentParser(),
                new TextDocumentParser()
        ));
    }

    /**
     * 配置默认文本分块器（语义分块）
     */
    @Bean
    public TextChunker textChunker() {
        // 使用语义分块策略，保持语义完整性
        return new SemanticChunker(1000, 100);
    }

    /**
     * 配置固定大小分块器
     */
    @Bean
    public TextChunker fixedSizeChunker() {
        return new FixedSizeChunker(1000, 100);
    }

    /**
     * 配置递归分块器
     */
    @Bean
    public TextChunker recursiveChunker() {
        return new RecursiveChunker(1000, 100);
    }

    /**
     * 复合文档解析器
     * 组合多个解析器，按优先级依次尝试
     */
    public static class CompositeDocumentParser implements DocumentParser {

        private final List<DocumentParser> parsers;

        public CompositeDocumentParser(List<DocumentParser> parsers) {
            this.parsers = parsers;
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
                    .flatMap(p -> Arrays.stream(p.getSupportedTypes()))
                    .distinct()
                    .toArray(String[]::new);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.File file) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(file.getName())) {
                    return parser.parse(file);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + file.getName());
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.File file, java.util.Map<String, Object> metadata) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(file.getName())) {
                    return parser.parse(file, metadata);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + file.getName());
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.InputStream inputStream, String fileName) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(fileName)) {
                    return parser.parse(inputStream, fileName);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + fileName);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(java.io.InputStream inputStream, String fileName, java.util.Map<String, Object> metadata) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(fileName)) {
                    return parser.parse(inputStream, fileName, metadata);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + fileName);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(byte[] bytes, String fileName) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(fileName)) {
                    return parser.parse(bytes, fileName);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + fileName);
        }

        @Override
        public com.aisaas.common.ai.document.Document parse(byte[] bytes, String fileName, java.util.Map<String, Object> metadata) {
            for (DocumentParser parser : parsers) {
                if (parser.supports(fileName)) {
                    return parser.parse(bytes, fileName, metadata);
                }
            }
            throw new UnsupportedOperationException("No parser supports file: " + fileName);
        }
    }
}
