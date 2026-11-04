package com.aisaas.rag.dto.doc;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocResponseDTO {
    private String docId;
    private String kbId;
    private String docName;
    private String docType;
    private Long docSize;
    private Integer pageCount;
    private Long charCount;
    private Integer chunkCount;
    private Integer processStatus;
    private String processStatusDesc;
    private Integer processProgress;
    private String processError;
    private Integer version;
    private Boolean isLatest;
    private LocalDateTime processStartedAt;
    private LocalDateTime processCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
