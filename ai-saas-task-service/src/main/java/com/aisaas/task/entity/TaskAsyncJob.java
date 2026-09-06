package com.aisaas.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task_async_job")
public class TaskAsyncJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String taskType;

    private String taskName;

    private Long userId;

    private Integer status;

    private Integer priority;

    private Integer progress;

    private String progressDetail;

    private String inputParams;

    private String outputResult;

    private String outputUrl;

    private String modelId;

    private String provider;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private BigDecimal costUsd;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime timeoutAt;

    private Integer retryCount;

    private Integer maxRetry;

    private String errorCode;

    private String errorMsg;

    private String stackTrace;

    private String workerNode;

    private String mqMessageId;

    private String traceId;

    private String callbackUrl;

    private Integer callbackStatus;

    private Integer callbackCount;

    private LocalDateTime lastCallbackAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
