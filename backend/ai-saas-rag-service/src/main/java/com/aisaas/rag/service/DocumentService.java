package com.aisaas.rag.service;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.doc.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface DocumentService {
    Result<DocUploadResponseDTO> uploadDocument(MultipartFile file, DocUploadDTO dto, Long userId);
    Result<DocResponseDTO> getDocument(String docId, Long userId);
    Result<IPage<DocResponseDTO>> listDocuments(DocQueryDTO queryDTO, Long userId);
    Result<Boolean> deleteDocument(String docId, Long userId);
    Result<Boolean> batchDeleteDocuments(List<String> docIds, Long userId);
    void downloadDocument(String docId, Long userId, HttpServletResponse response);
    Result<DocVersionDTO> getVersionHistory(String docId, Long userId);
    Result<Boolean> rollbackVersion(String docId, Long versionId, Long userId);
    Result<Boolean> reprocessDocument(String docId, Long userId);
    Result<DocProcessingStatusDTO> getProcessingStatus(String docId, Long userId);
}
