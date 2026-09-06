package com.aisaas.common.ai.document.parser;

import com.aisaas.common.ai.document.Document;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文档解析器抽象类
 * 提供通用的解析逻辑和工具方法
 */
@Slf4j
public abstract class AbstractDocumentParser implements DocumentParser {

    /**
     * 生成唯一ID
     */
    protected String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从文件名提取文件类型
     */
    protected String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 从文件路径提取文件名
     */
    protected String extractFileName(String filePath) {
        if (filePath == null) {
            return "";
        }
        int lastSeparator = filePath.lastIndexOf(File.separator);
        if (lastSeparator >= 0) {
            return filePath.substring(lastSeparator + 1);
        }
        return filePath;
    }

    /**
     * 创建基础文档对象
     */
    protected Document createBaseDocument(String content, String fileName, String fileType,
                                           Map<String, Object> metadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        Document document = Document.builder()
                .id(generateId())
                .content(content)
                .title(extractFileName(fileName))
                .docType(fileType)
                .source(fileName)
                .metadata(new HashMap<>(metadata))
                .createTime(System.currentTimeMillis())
                .build();

        document.calculateCharCount();

        // 添加默认元数据
        document.setMetadataValue("parser", getClass().getSimpleName());
        document.setMetadataValue("parseTime", System.currentTimeMillis());

        return document;
    }

    @Override
    public boolean supports(String fileName) {
        String fileType = extractFileType(fileName);
        return supportsType(fileType);
    }

    @Override
    public Document parse(File file) {
        return parse(file, null);
    }

    @Override
    public Document parse(File file, Map<String, Object> metadata) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return parse(inputStream, file.getName(), metadata);
        } catch (IOException e) {
            log.error("Failed to parse file: {}", file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to parse file: " + file.getName(), e);
        }
    }

    @Override
    public Document parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    @Override
    public Document parse(byte[] bytes, String fileName) {
        return parse(bytes, fileName, null);
    }

    @Override
    public Document parse(byte[] bytes, String fileName, Map<String, Object> metadata) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return parse(inputStream, fileName, metadata);
        } catch (IOException e) {
            log.error("Failed to parse byte array for file: {}", fileName, e);
            throw new RuntimeException("Failed to parse file: " + fileName, e);
        }
    }

    /**
     * 执行实际的解析逻辑（由子类实现）
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param metadata    元数据
     * @return 解析后的文档
     */
    protected abstract Document doParse(InputStream inputStream, String fileName,
                                       Map<String, Object> metadata);
}
