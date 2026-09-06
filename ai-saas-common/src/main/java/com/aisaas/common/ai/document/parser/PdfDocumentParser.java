package com.aisaas.common.ai.document.parser;

import com.aisaas.common.ai.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * PDF文档解析器
 * 支持 .pdf 格式
 */
@Slf4j
@Component
public class PdfDocumentParser extends AbstractDocumentParser {

    private static final String[] SUPPORTED_TYPES = {"pdf"};

    @Override
    public boolean supportsType(String fileType) {
        return fileType != null && "pdf".equalsIgnoreCase(fileType);
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
        try (PDDocument pdfDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            // 提取文本内容
            PDFTextStripper textStripper = new PDFTextStripper();
            String content = textStripper.getText(pdfDocument);

            // 提取元数据
            PDDocumentInformation docInfo = pdfDocument.getDocumentInformation();

            // 创建文档对象
            Document document = createBaseDocument(content, fileName, "pdf", metadata);

            // 设置标题
            if (docInfo.getTitle() != null && !docInfo.getTitle().isEmpty()) {
                document.setTitle(docInfo.getTitle());
            }

            // 添加PDF特有的元数据
            document.setMetadataValue("pdf.title", docInfo.getTitle());
            document.setMetadataValue("pdf.author", docInfo.getAuthor());
            document.setMetadataValue("pdf.subject", docInfo.getSubject());
            document.setMetadataValue("pdf.keywords", docInfo.getKeywords());
            document.setMetadataValue("pdf.creator", docInfo.getCreator());
            document.setMetadataValue("pdf.producer", docInfo.getProducer());
            document.setMetadataValue("pdf.pages", pdfDocument.getNumberOfPages());

            log.debug("Parsed PDF document: {} with {} pages and {} characters",
                    fileName, pdfDocument.getNumberOfPages(), document.getCharCount());

            return document;

        } catch (IOException e) {
            log.error("Failed to parse PDF file: {}", fileName, e);
            throw new RuntimeException("Failed to parse PDF file: " + fileName, e);
        }
    }
}
