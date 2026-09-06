package com.aisaas.task.dto;

import lombok.Data;

@Data
public class TaskUpdateDTO {

    private String taskName;

    private Integer priority;

    private String callbackUrl;
}
