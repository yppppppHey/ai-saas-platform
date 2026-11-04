package com.aisaas.rag.dto.doc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocUploadResponseDTO {
    private String docId;
    private String docName;
    private Integer chunkCount;
    private Integer indexedCount;
    private String status;
    private String message;
}
