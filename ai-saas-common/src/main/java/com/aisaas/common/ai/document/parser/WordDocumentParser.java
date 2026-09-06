package com.aisaas.common.ai.document.parser;

import com.aisaas.common.ai.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Word文档解析器
 * 支持 .docx 和 .doc 格式
 */
@Slf4j
@Component
public class WordDocumentParser extends AbstractDocumentParser {

    private static final String[] SUPPORTED_TYPES = {"docx", "doc"};

    @Override
    public boolean supportsType(String fileType) {
        if (fileType == null) {
            return false;
        }
        String lowerType = fileType.toLowerCase();
        return "docx".equals(lowerType) || "doc".equals(lowerType);
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
        String fileType = extractFileType(fileName);
        
        try {
            if ("docx".equalsIgnoreCase(fileType)) {
                return parseDocx(inputStream, fileName, metadata);
            } else if ("doc".equalsIgnoreCase(fileType)) {
                return parseDoc(inputStream, fileName, metadata);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
            }
        } catch (Exception e) {
            log.error("Failed to parse Word file: {}", fileName, e);
            throw new RuntimeException("Failed to parse Word file: " + fileName, e);
        }
    }

    /**
     * 解析 DOCX 格式
     */
    private Document parseDocx(InputStream inputStream, String fileName, Map<String, Object> metadata) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            
            // 提取所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            // 创建文档对象
            Document doc = createBaseDocument(content.toString().trim(), fileName, "docx", metadata);

            // 提取元数据
            try {
                POIXMLProperties props = document.getProperties();
                POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();
                
                if (coreProps.getTitle() != null && !coreProps.getTitle().isEmpty()) {
                    doc.setTitle(coreProps.getTitle());
                }
                
                doc.setMetadataValue("docx.title", coreProps.getTitle());
                doc.setMetadataValue("docx.author", coreProps.getCreator());
                doc.setMetadataValue("docx.subject", coreProps.getSubject());
                doc.setMetadataValue("docx.keywords", coreProps.getKeywords());
                doc.setMetadataValue("docx.category", coreProps.getCategory());
                doc.setMetadataValue("docx.paragraphs", paragraphs.size());
            } catch (Exception e) {
                log.warn("Failed to extract DOCX metadata for file: {}", fileName, e);
            }

            log.debug("Parsed DOCX document: {} with {} paragraphs and {} characters",
                    fileName, paragraphs.size(), doc.getCharCount());

            return doc;
        }
    }

    /**
     * 解析 DOC 格式（旧版Word）
     */
    private Document parseDoc(InputStream inputStream, String fileName, Map<String, Object> metadata) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            
            // 提取文本内容
            String content = extractor.getText();
            
            // 创建文档对象
            Document doc = createBaseDocument(content != null ? content.trim() : "", fileName, "doc", metadata);

            // 提取元数据
            try {
                doc.setTitle(document.getSummaryInformation().getTitle());
                doc.setMetadataValue("doc.author", document.getSummaryInformation().getAuthor());
                doc.setMetadataValue("doc.subject", document.getSummaryInformation().getSubject());
                doc.setMetadataValue("doc.keywords", document.getSummaryInformation().getKeywords());
                doc.setMetadataValue("doc.pages", document.getSummaryInformation().getPageCount());
            } catch (Exception e) {
                log.warn("Failed to extract DOC metadata for file: {}", fileName, e);
            }

            log.debug("Parsed DOC document: {} with {} characters", fileName, doc.getCharCount());

            return doc;
        }
    }
}
