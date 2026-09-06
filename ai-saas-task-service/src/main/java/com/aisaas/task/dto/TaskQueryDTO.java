package com.aisaas.task.dto;

import lombok.Data;

@Data
public class TaskQueryDTO {

    private Long userId;

    private String taskType;

    private Integer status;

    private String startDate;

    private String endDate;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
