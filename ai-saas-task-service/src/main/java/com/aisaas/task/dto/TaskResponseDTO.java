package com.aisaas.task.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TaskResponseDTO {

    private String taskId;

    private String taskType;

    private String taskName;

    private Integer status;

    private String statusDesc;

    private Integer priority;

    private Integer progress;

    private String progressDetail;

    private Map<String, Object> inputParams;

    private Object outputResult;

    private String outputUrl;

    private String modelId;

    private String provider;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private BigDecimal costUsd;

    private Long latencyMs;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private Integer retryCount;

    private String workerNode;

    private String traceId;
}
