package com.aisaas.task.schedule;

import com.aisaas.common.util.RedisKeys;
import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.mapper.TaskAsyncJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskTimeoutChecker {

    @Autowired
    private TaskAsyncJobMapper taskMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long TASK_TIMEOUT_MINUTES = 30;

    private static final long CHECK_INTERVAL_MS = 60000;

    @Scheduled(fixedDelay = CHECK_INTERVAL_MS)
    public void checkTimeoutTasks() {
        try {
            log.debug("开始检查超时任务");

            List<TaskAsyncJob> timeoutTasks = taskMapper.selectTimeoutTasks();

            if (timeoutTasks.isEmpty()) {
                log.debug("没有发现超时任务");
                return;
            }

            log.info("发现{}个超时任务", timeoutTasks.size());

            for (TaskAsyncJob task : timeoutTasks) {
                handleTimeoutTask(task);
            }
        } catch (Exception e) {
            log.error("检查超时任务失败: error={}", e.getMessage(), e);
        }
    }

    private void handleTimeoutTask(TaskAsyncJob task) {
        try {
            String taskId = task.getTaskId();
            LocalDateTime startedAt = task.getStartedAt();

            if (startedAt == null) {
                startedAt = task.getCreatedAt();
            }

            long elapsedMinutes = ChronoUnit.MINUTES.between(startedAt, LocalDateTime.now());

            if (elapsedMinutes < TASK_TIMEOUT_MINUTES) {
                log.debug("任务未超时: taskId={}, elapsed={}min", taskId, elapsedMinutes);
                return;
            }

            log.warn("处理超时任务: taskId={}, elapsed={}min, timeout={}min",
                    taskId, elapsedMinutes, TASK_TIMEOUT_MINUTES);

            task.setStatus(TaskStatusEnum.TIMEOUT.getCode());
            task.setErrorCode("TASK_TIMEOUT");
            task.setErrorMsg(String.format("任务执行超时，已运行%d分钟，超过最大限制%d分钟",
                    elapsedMinutes, TASK_TIMEOUT_MINUTES));
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            taskMapper.updateById(task);

            String timeoutKey = getTaskTimeoutKey(taskId);
            redisTemplate.opsForValue().set(timeoutKey, String.valueOf(System.currentTimeMillis()),
                    24, TimeUnit.HOURS);

            log.info("超时任务处理完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("处理超时任务失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
        }
    }

    public boolean isTaskTimeout(String taskId) {
        try {
            String timeoutKey = getTaskTimeoutKey(taskId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(timeoutKey));
        } catch (Exception e) {
            log.error("检查任务超时状态失败: taskId={}, error={}", taskId, e.getMessage());
            return false;
        }
    }

    public void clearTaskTimeoutFlag(String taskId) {
        try {
            String timeoutKey = getTaskTimeoutKey(taskId);
            redisTemplate.delete(timeoutKey);
            log.debug("清除任务超时标记: taskId={}", taskId);
        } catch (Exception e) {
            log.error("清除任务超时标记失败: taskId={}, error={}", taskId, e.getMessage());
        }
    }

    private String getTaskTimeoutKey(String taskId) {
        return RedisKeys.taskProgress(taskId) + ":timeout";
    }
}
