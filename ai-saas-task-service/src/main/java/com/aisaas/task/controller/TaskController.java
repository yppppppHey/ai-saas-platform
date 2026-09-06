package com.aisaas.task.controller;

import com.aisaas.common.result.Result;
import com.aisaas.task.dto.*;
import com.aisaas.task.service.TaskProgressService;
import com.aisaas.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskProgressService progressService;

    @PostMapping
    public Result<TaskResponseDTO> createTask(@Valid @RequestBody TaskCreateDTO dto) {
        Long userId = getCurrentUserId();
        log.info("创建任务: userId={}, taskType={}", userId, dto.getTaskType());
        return taskService.createTask(dto, userId);
    }

    @GetMapping("/{taskId}")
    public Result<TaskResponseDTO> getTask(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.debug("获取任务详情: taskId={}, userId={}", taskId, userId);
        return taskService.getTask(taskId, userId);
    }

    @GetMapping
    public Result<?> listTasks(TaskQueryDTO queryDTO) {
        Long userId = getCurrentUserId();
        queryDTO.setUserId(userId);
        log.debug("查询任务列表: userId={}, pageNum={}, pageSize={}",
                userId, queryDTO.getPageNum(), queryDTO.getPageSize());
        return taskService.listTasks(queryDTO, userId);
    }

    @PutMapping("/{taskId}/cancel")
    public Result<Boolean> cancelTask(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.info("取消任务: taskId={}, userId={}", taskId, userId);
        return taskService.cancelTask(taskId, userId);
    }

    @PutMapping("/{taskId}/retry")
    public Result<Boolean> retryTask(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.info("重试任务: taskId={}, userId={}", taskId, userId);
        return taskService.retryTask(taskId, userId);
    }

    @GetMapping("/{taskId}/progress")
    public Result<TaskProgressDTO> getTaskProgress(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.debug("获取任务进度: taskId={}, userId={}", taskId, userId);
        return taskService.getTaskProgress(taskId, userId);
    }

    @GetMapping(value = "/{taskId}/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskProgress(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.info("订阅任务进度流: taskId={}, userId={}", taskId, userId);
        return progressService.subscribeProgressStream(taskId);
    }

    @GetMapping("/statistics")
    public Result<TaskStatisticsDTO> getTaskStatistics() {
        Long userId = getCurrentUserId();
        log.debug("获取任务统计: userId={}", userId);
        return taskService.getTaskStatistics(userId);
    }

    @DeleteMapping("/{taskId}")
    public Result<Boolean> deleteTask(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        log.info("删除任务: taskId={}, userId={}", taskId, userId);
        return taskService.deleteTask(taskId, userId);
    }

    @DeleteMapping("/batch")
    public Result<Boolean> batchDeleteTasks(@RequestBody List<String> taskIds) {
        Long userId = getCurrentUserId();
        log.info("批量删除任务: taskIds={}, userId={}", taskIds, userId);
        return taskService.batchDeleteTasks(taskIds, userId);
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
