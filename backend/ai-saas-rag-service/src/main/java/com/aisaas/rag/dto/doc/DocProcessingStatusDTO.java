package com.aisaas.rag.dto.doc;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocProcessingStatusDTO {
    private String docId;
    private String docName;
    private Integer status;
    private String statusDesc;
    private Integer progress;
    private String currentStage;
    private String errorMessage;
    private Integer chunkCount;
    private Integer indexedCount;
    private List<ProcessingStage> stages;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    @Data
    public static class ProcessingStage {
        private String name;
        private String status;
        private Integer progress;
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
