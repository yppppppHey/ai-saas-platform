package com.aisaas.task.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaskStatisticsDTO {

    private Long totalTasks;

    private Long pendingTasks;

    private Long runningTasks;

    private Long successTasks;

    private Long failedTasks;

    private Long cancelledTasks;

    private Long timeoutTasks;

    private BigDecimal totalCostUsd;
}
