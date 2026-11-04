package com.aisaas.rag.dto.kb;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbResponseDTO {
    private String kbId;
    private String kbName;
    private String description;
    private Long userId;
    private String embeddingModel;
    private String vectorStore;
    private Integer dimension;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer totalDocuments;
    private Integer totalChunks;
    private Long totalSize;
    private Integer accessLevel;
    private String accessLevelDesc;
    private Long teamId;
    private Integer status;
    private String statusDesc;
    private Integer buildProgress;
    private LocalDateTime lastBuildAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
