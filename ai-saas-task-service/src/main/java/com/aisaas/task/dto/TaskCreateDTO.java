package com.aisaas.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class TaskCreateDTO {

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    private String taskName;

    private Integer priority = 5;

    @NotNull(message = "输入参数不能为空")
    private Map<String, Object> inputParams;

    private String modelId;

    private String callbackUrl;
}
