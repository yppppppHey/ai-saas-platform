package com.aisaas.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TaskProgressDTO {

    private String taskId;

    private Integer status;

    private String statusDesc;

    private Integer progress;

    private String progressDetail;

    private String stage;

    private Integer stageProgress;

    private Integer estimatedRemainingTime;

    private List<Map<String, Object>> completedSteps;

    private List<Map<String, Object>> pendingSteps;

    private String workerNode;

    private LocalDateTime startedAt;

    private LocalDateTime updatedAt;
}
