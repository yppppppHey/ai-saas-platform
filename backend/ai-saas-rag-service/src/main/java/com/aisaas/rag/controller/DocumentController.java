package com.aisaas.rag.controller;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.doc.*;
import com.aisaas.rag.service.DocumentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    public Result<DocUploadResponseDTO> uploadDocument(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute DocUploadDTO dto) {
        Long userId = getCurrentUserId();
        dto.setKbId(kbId);
        log.info("上传文档: kbId={}, userId={}, fileName={}", kbId, userId, file.getOriginalFilename());
        return documentService.uploadDocument(file, dto, userId);
    }

    @GetMapping("/{docId}")
    public Result<DocResponseDTO> getDocument(@PathVariable String kbId, @PathVariable String docId) {
        Long userId = getCurrentUserId();
        log.debug("获取文档详情: kbId={}, docId={}, userId={}", kbId, docId, userId);
        return documentService.getDocument(docId, userId);
    }

    @GetMapping
    public Result<?> listDocuments(@PathVariable String kbId, DocQueryDTO queryDTO) {
        Long userId = getCurrentUserId();
        queryDTO.setKbId(kbId);
        log.debug("查询文档列表: kbId={}, userId={}", kbId, userId);
        return documentService.listDocuments(queryDTO, userId);
    }

    @DeleteMapping("/{docId}")
    public Result<Boolean> deleteDocument(@PathVariable String kbId, @PathVariable String docId) {
        Long userId = getCurrentUserId();
        log.info("删除文档: kbId={}, docId={}, userId={}", kbId, docId, userId);
        return documentService.deleteDocument(docId, userId);
    }

    @DeleteMapping("/batch")
    public Result<Boolean> batchDeleteDocuments(@PathVariable String kbId, @RequestBody List<String> docIds) {
        Long userId = getCurrentUserId();
        log.info("批量删除文档: kbId={}, docCount={}, userId={}", kbId, docIds.size(), userId);
        return documentService.batchDeleteDocuments(docIds, userId);
    }

    @GetMapping("/{docId}/download")
    public void downloadDocument(@PathVariable String kbId, @PathVariable String docId, HttpServletResponse response) {
        Long userId = getCurrentUserId();
        log.info("下载文档: kbId={}, docId={}, userId={}", kbId, docId, userId);
        documentService.downloadDocument(docId, userId, response);
    }

    @GetMapping("/{docId}/versions")
    public Result<DocVersionDTO> getVersionHistory(@PathVariable String kbId, @PathVariable String docId) {
        Long userId = getCurrentUserId();
        log.debug("获取文档版本历史: kbId={}, docId={}, userId={}", kbId, docId, userId);
        return documentService.getVersionHistory(docId, userId);
    }

    @PostMapping("/{docId}/rollback/{versionId}")
    public Result<Boolean> rollbackVersion(@PathVariable String kbId, @PathVariable String docId, @PathVariable Long versionId) {
        Long userId = getCurrentUserId();
        log.info("回滚文档版本: kbId={}, docId={}, versionId={}, userId={}", kbId, docId, versionId, userId);
        return documentService.rollbackVersion(docId, versionId, userId);
    }

    @PostMapping("/{docId}/reprocess")
    public Result<Boolean> reprocessDocument(@PathVariable String kbId, @PathVariable String docId) {
        Long userId = getCurrentUserId();
        log.info("重新处理文档: kbId={}, docId={}, userId={}", kbId, docId, userId);
        return documentService.reprocessDocument(docId, userId);
    }

    @GetMapping("/{docId}/processing-status")
    public Result<DocProcessingStatusDTO> getProcessingStatus(@PathVariable String kbId, @PathVariable String docId) {
        Long userId = getCurrentUserId();
        log.debug("获取文档处理状态: kbId={}, docId={}, userId={}", kbId, docId, userId);
        return documentService.getProcessingStatus(docId, userId);
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
