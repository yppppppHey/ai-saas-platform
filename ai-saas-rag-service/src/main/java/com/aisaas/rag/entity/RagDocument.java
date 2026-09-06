package com.aisaas.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document")
public class RagDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String docId;

    private Long kbId;

    private Long userId;

    private String docName;

    private String docType;

    private Long docSize;

    private String storageKey;

    private String storageType;

    private String encoding;

    private Integer pageCount;

    private Long charCount;

    private Integer chunkCount;

    private String extractionMethod;

    private String extractionResult;

    private Integer processStatus;

    private Integer processProgress;

    private LocalDateTime processStartedAt;

    private LocalDateTime processCompletedAt;

    private String processError;

    private Integer version;

    private Integer isLatest;

    private Long previousVersionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
