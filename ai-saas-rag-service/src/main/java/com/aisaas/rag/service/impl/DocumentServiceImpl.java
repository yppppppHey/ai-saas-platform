package com.aisaas.rag.service.impl;

import com.aisaas.common.result.Result;
import com.aisaas.common.constant.ResultCode;
import com.aisaas.rag.constant.DocProcessStatusEnum;
import com.aisaas.rag.dto.doc.*;
import com.aisaas.rag.entity.RagDocument;
import com.aisaas.rag.mapper.RagDocumentMapper;
import com.aisaas.rag.service.DocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private RagDocumentMapper documentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DocUploadResponseDTO> uploadDocument(MultipartFile file, DocUploadDTO dto, Long userId) {
        try {
            String docId = "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            String originalFilename = file.getOriginalFilename();
            String docType = getFileExtension(originalFilename);

            RagDocument document = new RagDocument();
            document.setDocId(docId);
            document.setKbId(Long.parseLong(dto.getKbId()));
            document.setUserId(userId);
            document.setDocName(originalFilename);
            document.setDocType(docType);
            document.setDocSize(file.getSize());
            document.setStorageKey("rag/" + dto.getKbId() + "/" + docId);
            document.setStorageType("minio");
            document.setChunkCount(0);
            document.setProcessStatus(DocProcessStatusEnum.PENDING.getCode());
            document.setProcessProgress(0);
            document.setVersion(1);
            document.setIsLatest(1);
            document.setIsDeleted(0);
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());

            documentMapper.insert(document);

            log.info("文档上传成功: docId={}, userId={}, kbId={}", docId, userId, dto.getKbId());

            return Result.success(DocUploadResponseDTO.builder()
                    .docId(docId)
                    .docName(originalFilename)
                    .chunkCount(0)
                    .indexedCount(0)
                    .status(DocProcessStatusEnum.PENDING.getName())
                    .message("文档上传成功，等待处理")
                    .build());

        } catch (Exception e) {
            log.error("文档上传失败: error={}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "文档上传失败: " + e.getMessage());
        }
    }

    @Override
    public Result<DocResponseDTO> getDocument(String docId, Long userId) {
        RagDocument document = documentMapper.selectByDocId(docId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }
        return Result.success(convertToDTO(document));
    }

    @Override
    public Result<IPage<DocResponseDTO>> listDocuments(DocQueryDTO queryDTO, Long userId) {
        Page<RagDocument> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        Long kbId = queryDTO.getKbId() != null ? Long.parseLong(queryDTO.getKbId()) : null;
        IPage<RagDocument> docPage = documentMapper.selectDocPage(
                page, kbId, userId, queryDTO.getDocType(),
                queryDTO.getProcessStatus(), queryDTO.getKeyword()
        );

        List<DocResponseDTO> dtoList = docPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<DocResponseDTO> resultPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        resultPage.setRecords(dtoList);

        return Result.success(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteDocument(String docId, Long userId) {
        RagDocument document = documentMapper.selectByDocId(docId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }
        if (!document.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权删除该文档");
        }

        document.setIsDeleted(1);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        log.info("删除文档成功: docId={}, userId={}", docId, userId);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchDeleteDocuments(List<String> docIds, Long userId) {
        int deleted = 0;
        int failed = 0;

        for (String docId : docIds) {
            try {
                Result<Boolean> result = deleteDocument(docId, userId);
                if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                    deleted++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("批量删除文档失败: docId={}, error={}", docId, e.getMessage());
                failed++;
            }
        }

        log.info("批量删除文档完成: deleted={}, failed={}", deleted, failed);
        return Result.success(true);
    }

    @Override
    public void downloadDocument(String docId, Long userId, HttpServletResponse response) {
        Result.error(ResultCode.NOT_IMPLEMENTED, "功能开发中");
    }

    @Override
    public Result<DocVersionDTO> getVersionHistory(String docId, Long userId) {
        return Result.success(new DocVersionDTO());
    }

    @Override
    public Result<Boolean> rollbackVersion(String docId, Long versionId, Long userId) {
        return Result.success(true);
    }

    @Override
    public Result<Boolean> reprocessDocument(String docId, Long userId) {
        RagDocument document = documentMapper.selectByDocId(docId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }

        document.setProcessStatus(DocProcessStatusEnum.PENDING.getCode());
        document.setProcessProgress(0);
        document.setProcessError(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        log.info("重新处理文档: docId={}, userId={}", docId, userId);
        return Result.success(true);
    }

    @Override
    public Result<DocProcessingStatusDTO> getProcessingStatus(String docId, Long userId) {
        RagDocument document = documentMapper.selectByDocId(docId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }

        DocProcessingStatusDTO status = new DocProcessingStatusDTO();
        status.setDocId(docId);
        status.setDocName(document.getDocName());
        status.setStatus(document.getProcessStatus());
        status.setStatusDesc(DocProcessStatusEnum.fromCode(document.getProcessStatus()).getName());
        status.setProgress(document.getProcessProgress());
        status.setChunkCount(document.getChunkCount());
        status.setIndexedCount(0);
        status.setStartedAt(document.getProcessStartedAt());
        status.setUpdatedAt(document.getUpdatedAt());

        return Result.success(status);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private DocResponseDTO convertToDTO(RagDocument document) {
        DocResponseDTO dto = new DocResponseDTO();
        BeanUtils.copyProperties(document, dto);
        dto.setDocId(document.getDocId());
        dto.setKbId(document.getKbId().toString());
        dto.setProcessStatusDesc(DocProcessStatusEnum.fromCode(document.getProcessStatus()).getName());
        return dto;
    }
}
