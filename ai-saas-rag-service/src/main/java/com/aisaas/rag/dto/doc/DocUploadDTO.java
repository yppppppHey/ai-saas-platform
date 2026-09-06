package com.aisaas.rag.dto.doc;

import lombok.Data;

@Data
public class DocUploadDTO {
    private String kbId;
    private String description;
    private String extractionMethod;
    private Integer chunkSize;
    private Integer chunkOverlap;
}
