package com.aisaas.task.service.impl;

import com.aisaas.common.exception.BizException;
import com.aisaas.common.result.Result;
import com.aisaas.common.constant.ResultCode;
import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.constant.TaskTypeEnum;
import com.aisaas.task.dto.*;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.mapper.TaskAsyncJobMapper;
import com.aisaas.task.mq.TaskMQProducer;
import com.aisaas.task.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskAsyncJobMapper taskMapper;

    @Autowired
    private TaskMQProducer taskMQProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TaskResponseDTO> createTask(TaskCreateDTO dto, Long userId) {
        TaskTypeEnum taskType = TaskTypeEnum.fromCode(dto.getTaskType());
        if (taskType == null) {
            throw new BizException("无效的任务类型: " + dto.getTaskType());
        }

        String taskId = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        TaskAsyncJob task = new TaskAsyncJob();
        task.setTaskId(taskId);
        task.setTaskType(dto.getTaskType());
        task.setTaskName(dto.getTaskName() != null ? dto.getTaskName() : taskType.getName());
        task.setUserId(userId);
        task.setStatus(TaskStatusEnum.PENDING.getCode());
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : 5);
        task.setProgress(0);
        task.setInputParams(JsonUtils.toJson(dto.getInputParams()));
        task.setModelId(dto.getModelId());
        task.setMaxRetry(3);
        task.setRetryCount(0);
        task.setCallbackUrl(dto.getCallbackUrl());
        task.setCallbackStatus(0);
        task.setIsDeleted(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.insert(task);

        try {
            taskMQProducer.sendTaskMessage(task);
            task.setStatus(TaskStatusEnum.QUEUED.getCode());
            taskMapper.updateStatus(taskId, TaskStatusEnum.QUEUED.getCode());
        } catch (Exception e) {
            log.error("发送任务消息失败: taskId={}, error={}", taskId, e.getMessage());
        }

        log.info("创建异步任务成功: taskId={}, userId={}, taskType={}", taskId, userId, dto.getTaskType());
        return Result.success(convertToDTO(task));
    }

    @Override
    public Result<TaskResponseDTO> getTask(String taskId, Long userId) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "任务不存在");
        }

        if (!task.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该任务");
        }

        return Result.success(convertToDTO(task));
    }

    @Override
    public Result<IPage<TaskResponseDTO>> listTasks(TaskQueryDTO queryDTO, Long userId) {
        Page<TaskAsyncJob> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        IPage<TaskAsyncJob> taskPage = taskMapper.selectTaskPage(
                page,
                userId,
                queryDTO.getStatus(),
                queryDTO.getTaskType()
        );

        List<TaskResponseDTO> dtoList = taskPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<TaskResponseDTO> resultPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        resultPage.setRecords(dtoList);

        return Result.success(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> cancelTask(String taskId, Long userId) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "任务不存在");
        }

        if (!task.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作该任务");
        }

        if (!TaskStatusEnum.canCancel(task.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前状态不可取消: " + TaskStatusEnum.fromCode(task.getStatus()).getDesc());
        }

        task.setStatus(TaskStatusEnum.CANCELLED.getCode());
        task.setCancelledAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("取消任务成功: taskId={}, userId={}", taskId, userId);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> retryTask(String taskId, Long userId) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "任务不存在");
        }

        if (!task.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作该任务");
        }

        if (!TaskStatusEnum.canRetry(task.getStatus())) {
            return Result.error(ResultCode.BUSINESS_ERROR, "当前状态不支持重试: " + TaskStatusEnum.fromCode(task.getStatus()).getDesc());
        }

        if (task.getRetryCount() >= task.getMaxRetry()) {
            return Result.error(ResultCode.BUSINESS_ERROR, "已达到最大重试次数");
        }

        task.setStatus(TaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setProgressDetail("任务已重新提交");
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        try {
            taskMQProducer.sendTaskMessage(task);
            taskMapper.updateStatus(taskId, TaskStatusEnum.QUEUED.getCode());
        } catch (Exception e) {
            log.error("发送重试任务消息失败: taskId={}, error={}", taskId, e.getMessage());
        }

        log.info("重试任务成功: taskId={}, userId={}, retryCount={}", taskId, userId, task.getRetryCount());
        return Result.success(true);
    }

    @Override
    public Result<TaskProgressDTO> getTaskProgress(String taskId, Long userId) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "任务不存在");
        }

        if (!task.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该任务");
        }

        TaskProgressDTO progressDTO = new TaskProgressDTO();
        progressDTO.setTaskId(taskId);
        progressDTO.setStatus(task.getStatus());
        progressDTO.setStatusDesc(TaskStatusEnum.fromCode(task.getStatus()).getDesc());
        progressDTO.setProgress(task.getProgress());
        progressDTO.setProgressDetail(task.getProgressDetail());
        progressDTO.setWorkerNode(task.getWorkerNode());
        progressDTO.setStartedAt(task.getStartedAt());
        progressDTO.setUpdatedAt(task.getUpdatedAt());

        return Result.success(progressDTO);
    }

    @Override
    public Result<TaskStatisticsDTO> getTaskStatistics(Long userId) {
        List<TaskAsyncJob> tasks = taskMapper.selectByUserId(userId);

        TaskStatisticsDTO stats = new TaskStatisticsDTO();
        stats.setTotalTasks((long) tasks.size());
        stats.setPendingTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.PENDING.getCode()).count());
        stats.setRunningTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.RUNNING.getCode()).count());
        stats.setSuccessTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.SUCCESS.getCode()).count());
        stats.setFailedTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.FAILED.getCode()).count());
        stats.setCancelledTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.CANCELLED.getCode()).count());
        stats.setTimeoutTasks(tasks.stream().filter(t -> t.getStatus() == TaskStatusEnum.TIMEOUT.getCode()).count());

        BigDecimal totalCost = tasks.stream()
                .map(TaskAsyncJob::getCostUsd)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalCostUsd(totalCost);

        return Result.success(stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteTask(String taskId, Long userId) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "任务不存在");
        }

        if (!task.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作该任务");
        }

        task.setIsDeleted(1);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("删除任务成功: taskId={}, userId={}", taskId, userId);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchDeleteTasks(List<String> taskIds, Long userId) {
        int deleted = 0;
        int failed = 0;

        for (String taskId : taskIds) {
            try {
                Result<Boolean> result = deleteTask(taskId, userId);
                if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                    deleted++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("批量删除任务失败: taskId={}, error={}", taskId, e.getMessage());
                failed++;
            }
        }

        log.info("批量删除任务完成: deleted={}, failed={}", deleted, failed);
        return Result.success(true);
    }

    private TaskResponseDTO convertToDTO(TaskAsyncJob task) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTaskType(task.getTaskType());
        dto.setTaskName(task.getTaskName());
        dto.setStatus(task.getStatus());
        dto.setStatusDesc(TaskStatusEnum.fromCode(task.getStatus()).getDesc());
        dto.setPriority(task.getPriority());
        dto.setProgress(task.getProgress());
        dto.setProgressDetail(task.getProgressDetail());

        if (task.getInputParams() != null) {
            dto.setInputParams(JsonUtils.parseObject(task.getInputParams(), java.util.Map.class));
        }

        if (task.getOutputResult() != null) {
            dto.setOutputResult(JsonUtils.parseObject(task.getOutputResult(), java.util.Map.class));
        }

        dto.setOutputUrl(task.getOutputUrl());
        dto.setModelId(task.getModelId());
        dto.setProvider(task.getProvider());
        dto.setPromptTokens(task.getPromptTokens());
        dto.setCompletionTokens(task.getCompletionTokens());
        dto.setTotalTokens(task.getTotalTokens());
        dto.setCostUsd(task.getCostUsd());

        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            dto.setLatencyMs(java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis());
        }

        dto.setStartedAt(task.getStartedAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setRetryCount(task.getRetryCount());
        dto.setWorkerNode(task.getWorkerNode());
        dto.setTraceId(task.getTraceId());

        return dto;
    }
}
