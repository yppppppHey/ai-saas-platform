package com.aisaas.task.engine.state;

import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.mapper.TaskAsyncJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TaskStateMachine {

    private final Set<String> executingTasks = ConcurrentHashMap.newKeySet();

    @Autowired
    private TaskAsyncJobMapper taskMapper;

    private static final Set<Integer> FINAL_STATES = Set.of(
            TaskStatusEnum.SUCCESS.getCode(),
            TaskStatusEnum.FAILED.getCode(),
            TaskStatusEnum.CANCELLED.getCode(),
            TaskStatusEnum.TIMEOUT.getCode()
    );

    private static final Set<Integer> CANCELLABLE_STATES = Set.of(
            TaskStatusEnum.PENDING.getCode(),
            TaskStatusEnum.QUEUED.getCode(),
            TaskStatusEnum.RUNNING.getCode()
    );

    public boolean tryAcquire(String taskId) {
        boolean acquired = executingTasks.add(taskId);
        if (acquired) {
            log.debug("成功获取任务执行锁: taskId={}", taskId);
        } else {
            log.warn("任务正在执行中，无法获取执行锁: taskId={}", taskId);
        }
        return acquired;
    }

    public void release(String taskId) {
        boolean released = executingTasks.remove(taskId);
        if (released) {
            log.debug("释放任务执行锁: taskId={}", taskId);
        }
    }

    public boolean isExecuting(String taskId) {
        return executingTasks.contains(taskId);
    }

    public boolean canTransition(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        if (FINAL_STATES.contains(currentStatus)) {
            log.warn("终态任务不允许状态转换: currentStatus={}", currentStatus);
            return false;
        }

        if (currentStatus.equals(newStatus)) {
            return true;
        }

        return isValidTransition(currentStatus, newStatus);
    }

    private boolean isValidTransition(Integer current, Integer next) {
        return switch (current) {
            case 0 -> Set.of(1, 2, 4, 5, 6).contains(next);
            case 1 -> Set.of(2, 4, 5, 6).contains(next);
            case 2 -> Set.of(3, 4, 5, 6).contains(next);
            default -> false;
        };
    }

    public void transition(String taskId, TaskStatusEnum newStatus) {
        transition(taskId, newStatus.getCode());
    }

    public void transition(String taskId, Integer newStatus) {
        TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.error("任务不存在，无法转换状态: taskId={}", taskId);
            return;
        }

        Integer currentStatus = task.getStatus();
        if (!canTransition(currentStatus, newStatus)) {
            log.warn("非法状态转换: taskId={}, currentStatus={}, newStatus={}",
                    taskId, currentStatus, newStatus);
            return;
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());

        if (newStatus == TaskStatusEnum.RUNNING.getCode()) {
            task.setStartedAt(LocalDateTime.now());
        } else if (FINAL_STATES.contains(newStatus)) {
            task.setCompletedAt(LocalDateTime.now());
        }

        taskMapper.updateById(task);
        log.info("任务状态转换成功: taskId={}, {} -> {}",
                taskId, TaskStatusEnum.fromCode(currentStatus).getDesc(),
                TaskStatusEnum.fromCode(newStatus).getDesc());
    }

    public boolean canCancel(Integer status) {
        return CANCELLABLE_STATES.contains(status);
    }

    public boolean canRetry(Integer status) {
        return status == TaskStatusEnum.FAILED.getCode() ||
               status == TaskStatusEnum.TIMEOUT.getCode();
    }

    public boolean isFinalStatus(Integer status) {
        return FINAL_STATES.contains(status);
    }
}
