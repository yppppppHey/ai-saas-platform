package com.aisaas.task.controller;

import com.aisaas.common.result.Result;
import com.aisaas.task.constant.TaskTypeEnum;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks/types")
public class TaskTypeController {

    @GetMapping
    public Result<List<TaskTypeVO>> listTaskTypes() {
        List<TaskTypeVO> types = Arrays.stream(TaskTypeEnum.values())
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(types);
    }

    private TaskTypeVO convertToVO(TaskTypeEnum type) {
        TaskTypeVO vo = new TaskTypeVO();
        vo.setCode(type.getCode());
        vo.setName(type.getName());
        vo.setDescription(type.getDescription());
        return vo;
    }

    @Data
    public static class TaskTypeVO {
        private String code;
        private String name;
        private String description;
    }
}
