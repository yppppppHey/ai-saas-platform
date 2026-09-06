package com.aisaas.task.engine;

import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.constant.TaskStatusEnum;
import com.aisaas.task.engine.chain.TaskHandlerChain;
import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.engine.processor.TaskProcessor;
import com.aisaas.task.engine.processor.TaskProcessorFactory;
import com.aisaas.task.engine.state.TaskStateMachine;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.mapper.TaskAsyncJobMapper;
import com.aisaas.task.service.TaskProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class TaskExecutionEngine {

    @Autowired
    private TaskAsyncJobMapper taskMapper;

    @Autowired
    private TaskProcessorFactory processorFactory;

    @Autowired
    private TaskStateMachine stateMachine;

    @Autowired
    private TaskHandlerChain handlerChain;

    @Autowired
    private TaskProgressService progressService;

    @org.springframework.beans.factory.annotation.Qualifier("taskExecutor")
    @org.springframework.beans.factory.annotation.Autowired
    private ExecutorService executorService;

    public void executeTask(TaskAsyncJob task) {
        String taskId = task.getTaskId();

        try {
            boolean acquired = stateMachine.tryAcquire(taskId);
            if (!acquired) {
                log.warn("任务正在执行中，跳过重复执行: taskId={}", taskId);
                return;
            }

            TaskProcessor processor = processorFactory.getProcessor(task.getTaskType());
            if (processor == null) {
                throw new RuntimeException("未找到任务处理器: taskType=" + task.getTaskType());
            }

            TaskExecutionContext context = new TaskExecutionContext(task, processor);

            stateMachine.transition(taskId, TaskStatusEnum.RUNNING);
            task.setStatus(TaskStatusEnum.RUNNING.getCode());
            task.setStartedAt(LocalDateTime.now());
            task.setWorkerNode(getWorkerNode());
            taskMapper.updateById(task);

            progressService.updateProgress(taskId, 5, "任务开始执行");

            CompletableFuture.runAsync(() -> {
                try {
                    handlerChain.execute(context);
                    handleSuccess(taskId, context);
                } catch (Exception e) {
                    handleFailure(taskId, context, e);
                }
            }, executorService);

        } catch (Exception e) {
            log.error("任务执行引擎异常: taskId={}, error={}", taskId, e.getMessage(), e);
            stateMachine.release(taskId);
        }
    }

    private void handleSuccess(String taskId, TaskExecutionContext context) {
        try {
            TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
            if (task == null) return;

            task.setStatus(TaskStatusEnum.SUCCESS.getCode());
            task.setProgress(100);
            task.setProgressDetail("任务执行成功");
            task.setCompletedAt(LocalDateTime.now());

            if (context.getOutputResult() != null) {
                task.setOutputResult(JsonUtils.toJson(context.getOutputResult()));
            }

            updateTokenAndCost(task, context);
            taskMapper.updateById(task);

            progressService.updateProgress(taskId, 100, "任务执行完成");
            progressService.notifyTaskComplete(task);

            log.info("任务执行成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("处理任务成功状态失败: taskId={}, error={}", taskId, e.getMessage(), e);
        } finally {
            stateMachine.release(taskId);
        }
    }

    private void handleFailure(String taskId, TaskExecutionContext context, Exception e) {
        try {
            TaskAsyncJob task = taskMapper.selectByTaskId(taskId);
            if (task == null) return;

            int retryCount = task.getRetryCount() + 1;

            if (retryCount <= task.getMaxRetry()) {
                task.setRetryCount(retryCount);
                task.setStatus(TaskStatusEnum.PENDING.getCode());
                task.setProgress(0);
                task.setProgressDetail("准备第" + retryCount + "次重试");
                taskMapper.updateById(task);

                log.info("任务准备重试: taskId={}, retryCount={}", taskId, retryCount);
            } else {
                task.setStatus(TaskStatusEnum.FAILED.getCode());
                task.setErrorCode("TASK_EXECUTE_ERROR");
                task.setErrorMsg(e.getMessage());
                task.setStackTrace(getStackTraceString(e));
                task.setCompletedAt(LocalDateTime.now());
                taskMapper.updateById(task);

                progressService.notifyTaskFail(task, e.getMessage());

                log.error("任务执行失败: taskId={}, error={}", taskId, e.getMessage(), e);
            }
        } catch (Exception ex) {
            log.error("处理任务失败状态异常: taskId={}, error={}", taskId, ex.getMessage(), ex);
        } finally {
            stateMachine.release(taskId);
        }
    }

    private void updateTokenAndCost(TaskAsyncJob task, TaskExecutionContext context) {
        if (context.getTokenUsage() != null) {
            task.setPromptTokens(context.getTokenUsage().getPromptTokens());
            task.setCompletionTokens(context.getTokenUsage().getCompletionTokens());
            task.setTotalTokens(context.getTokenUsage().getTotalTokens());
        }

        if (context.getCostUsd() != null) {
            task.setCostUsd(context.getCostUsd());
        }
    }

    private String getWorkerNode() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "worker-unknown";
        }
    }

    private String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }
}
