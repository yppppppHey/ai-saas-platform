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
        try {
            RagDocument document = documentMapper.selectByDocId(docId);
            if (document == null || document.getIsDeleted() == 1) {
                writeJsonError(response, HttpServletResponse.SC_NOT_FOUND, "文档不存在");
                return;
            }
            if (!document.getUserId().equals(userId)) {
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "无权下载该文档");
                return;
            }

            // 优先返回解析出的文本内容；原始文件字节依赖 MinIO/OSS（尚未接入），无法直接回源
            String content = document.getExtractionResult();
            if (content == null || content.isBlank()) {
                writeJsonError(response, HttpServletResponse.SC_CONFLICT,
                        "文档原始文件未持久化存储（对象存储待接入），且无已解析文本可下载");
                return;
            }

            String fileName = document.getDocName() != null ? document.getDocName() : docId + ".txt";
            if (!fileName.contains(".")) {
                fileName = fileName + ".txt";
            }
            response.setContentType("text/plain; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                    + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("+", "%20"));
            response.getOutputStream().write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            response.getOutputStream().flush();

            log.info("文档下载成功: docId={}, userId={}, fileName={}", docId, userId, fileName);
        } catch (Exception e) {
            log.error("文档下载失败: docId={}, userId={}", docId, userId, e);
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "文档下载失败: " + e.getMessage());
        }
    }

    /**
     * 向客户端写回 JSON 错误信息（下载场景下不走统一 Result 包装）
     */
    private void writeJsonError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
        } catch (IOException e) {
            log.error("写回下载错误响应失败", e);
        }
    }

    @Override
    public Result<DocVersionDTO> getVersionHistory(String docId, Long userId) {
        RagDocument document = documentMapper.selectByDocId(docId);
        if (document == null || document.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }
        if (!document.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权查看该文档版本");
        }

        List<RagDocument> versionRows = documentMapper.selectVersionsByDocId(docId);
        DocVersionDTO dto = new DocVersionDTO();
        dto.setDocId(docId);
        dto.setDocName(document.getDocName());
        dto.setCurrentVersion(document.getVersion());
        dto.setVersions(versionRows.stream().map(row -> {
            DocVersionDTO.VersionInfo info = new DocVersionDTO.VersionInfo();
            info.setVersionId(row.getId());
            info.setVersion(row.getVersion());
            info.setSize(row.getDocSize());
            info.setCreatedAt(row.getCreatedAt());
            boolean latest = row.getIsLatest() != null && row.getIsLatest() == 1;
            info.setIsLatest(latest);
            info.setChangeLog(latest ? "当前版本" : "历史版本");
            return info;
        }).collect(Collectors.toList()));

        return Result.success(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> rollbackVersion(String docId, Long versionId, Long userId) {
        RagDocument current = documentMapper.selectByDocId(docId);
        if (current == null || current.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "文档不存在");
        }
        if (!current.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权回滚该文档");
        }

        RagDocument target = documentMapper.selectById(versionId);
        if (target == null || target.getIsDeleted() == 1 || !docId.equals(target.getDocId())) {
            return Result.error(ResultCode.NOT_FOUND, "目标版本不存在");
        }
        if (target.getId().equals(current.getId())) {
            return Result.error(ResultCode.BAD_REQUEST, "目标版本已是当前版本");
        }

        // 将目标版本的文档元数据与内容恢复到当前行，并递增版本号（保持 docId 唯一行的数据模型）
        Integer previousVersion = current.getVersion();
        current.setDocName(target.getDocName());
        current.setDocSize(target.getDocSize());
        current.setPageCount(target.getPageCount());
        current.setCharCount(target.getCharCount());
        current.setExtractionResult(target.getExtractionResult());
        current.setVersion(previousVersion != null ? previousVersion + 1 : 1);
        current.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(current);

        log.info("文档版本回滚成功: docId={}, userId={}, fromVersion={}, toVersionSnapshot={}, newVersion={}",
                docId, userId, previousVersion, target.getVersion(), current.getVersion());
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
