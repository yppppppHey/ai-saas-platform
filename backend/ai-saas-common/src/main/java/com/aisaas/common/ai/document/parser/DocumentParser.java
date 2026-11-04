package com.aisaas.common.ai.document.parser;

import com.aisaas.common.ai.document.Document;

import java.io.File;
import java.io.InputStream;

/**
 * 文档解析器接口
 * 定义文档解析的标准接口
 */
public interface DocumentParser {

    /**
     * 是否支持该文件类型
     *
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean supports(String fileName);

    /**
     * 是否支持该文件类型
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    boolean supportsType(String fileType);

    /**
     * 获取支持的文件类型
     *
     * @return 支持的文件类型数组
     */
    String[] getSupportedTypes();

    /**
     * 解析文件
     *
     * @param file 文件对象
     * @return 解析后的文档对象
     */
    Document parse(File file);

    /**
     * 解析文件
     *
     * @param file 文件对象
     * @param metadata 元数据
     * @return 解析后的文档对象
     */
    Document parse(File file, java.util.Map<String, Object> metadata);

    /**
     * 解析输入流
     *
     * @param inputStream 输入流
     * @param fileName 文件名（用于识别类型）
     * @return 解析后的文档对象
     */
    Document parse(InputStream inputStream, String fileName);

    /**
     * 解析输入流
     *
     * @param inputStream 输入流
     * @param fileName 文件名（用于识别类型）
     * @param metadata 元数据
     * @return 解析后的文档对象
     */
    Document parse(InputStream inputStream, String fileName, java.util.Map<String, Object> metadata);

    /**
     * 解析字节数组
     *
     * @param bytes 字节数组
     * @param fileName 文件名（用于识别类型）
     * @return 解析后的文档对象
     */
    Document parse(byte[] bytes, String fileName);

    /**
     * 解析字节数组
     *
     * @param bytes 字节数组
     * @param fileName 文件名（用于识别类型）
     * @param metadata 元数据
     * @return 解析后的文档对象
     */
    Document parse(byte[] bytes, String fileName, java.util.Map<String, Object> metadata);
}
