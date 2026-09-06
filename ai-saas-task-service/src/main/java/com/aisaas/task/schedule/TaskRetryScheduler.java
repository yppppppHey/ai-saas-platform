package com.aisaas.task.schedule;

import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.mapper.TaskAsyncJobMapper;
import com.aisaas.task.mq.TaskMQProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TaskRetryScheduler {

    @Autowired
    private TaskAsyncJobMapper taskMapper;

    @Autowired
    private TaskMQProducer taskMQProducer;

    @Scheduled(fixedDelay = 300000)
    public void retryFailedTasks() {
        try {
            log.debug("开始检查需要重试的失败任务");

            List<TaskAsyncJob> failedTasks = taskMapper.selectByStatusWithLimit(
                    TaskStatusEnum.FAILED.getCode(), 10);

            if (failedTasks.isEmpty()) {
                log.debug("没有发现需要重试的失败任务");
                return;
            }

            log.info("发现{}个需要重试的失败任务", failedTasks.size());

            for (TaskAsyncJob task : failedTasks) {
                processRetry(task);
            }
        } catch (Exception e) {
            log.error("重试失败任务调度异常: error={}", e.getMessage(), e);
        }
    }

    private void processRetry(TaskAsyncJob task) {
        try {
            String taskId = task.getTaskId();

            if (task.getRetryCount() >= task.getMaxRetry()) {
                log.warn("任务已达到最大重试次数，不再重试: taskId={}, retryCount={}",
                        taskId, task.getRetryCount());
                return;
            }

            int newRetryCount = task.getRetryCount() + 1;
            task.setRetryCount(newRetryCount);
            task.setStatus(TaskStatusEnum.PENDING.getCode());
            task.setProgress(0);
            task.setProgressDetail("准备第" + newRetryCount + "次重试");
            task.setUpdatedAt(LocalDateTime.now());
            task.setErrorMsg(null);
            task.setErrorCode(null);

            taskMapper.updateById(task);

            taskMQProducer.sendTaskMessage(task);

            log.info("任务重试已发送: taskId={}, retryCount={}", taskId, newRetryCount);
        } catch (Exception e) {
            log.error("处理任务重试失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
        }
    }
}
