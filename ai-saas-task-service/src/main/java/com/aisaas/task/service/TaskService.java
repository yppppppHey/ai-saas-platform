package com.aisaas.task.service;

import com.aisaas.common.result.Result;
import com.aisaas.task.dto.*;

public interface TaskService {

    Result<TaskResponseDTO> createTask(TaskCreateDTO dto, Long userId);

    Result<TaskResponseDTO> getTask(String taskId, Long userId);

    Result<com.baomidou.mybatisplus.core.metadata.IPage<TaskResponseDTO>> listTasks(TaskQueryDTO queryDTO, Long userId);

    Result<Boolean> cancelTask(String taskId, Long userId);

    Result<Boolean> retryTask(String taskId, Long userId);

    Result<TaskProgressDTO> getTaskProgress(String taskId, Long userId);

    Result<TaskStatisticsDTO> getTaskStatistics(Long userId);

    Result<Boolean> deleteTask(String taskId, Long userId);

    Result<Boolean> batchDeleteTasks(java.util.List<String> taskIds, Long userId);
}
