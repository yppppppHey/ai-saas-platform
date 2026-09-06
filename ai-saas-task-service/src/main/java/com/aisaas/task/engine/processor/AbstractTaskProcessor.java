package com.aisaas.task.engine.processor;

import com.aisaas.task.engine.context.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTaskProcessor implements TaskProcessor {

    @Override
    public abstract String getTaskType();

    @Override
    public void preProcess(TaskExecutionContext context) throws Exception {
        log.debug("执行默认预处理方法: taskId={}, taskType={}",
                context.getTask().getTaskId(), getTaskType());
    }

    @Override
    public abstract Object execute(TaskExecutionContext context) throws Exception;

    @Override
    public void postProcess(TaskExecutionContext context) throws Exception {
        log.debug("执行默认后处理方法: taskId={}, taskType={}",
                context.getTask().getTaskId(), getTaskType());
    }

    @Override
    public boolean supports(String taskType) {
        return getTaskType().equals(taskType);
    }

    protected void updateProgress(TaskExecutionContext context, int progress, String detail) {
        context.getTask().setProgress(progress);
        context.getTask().setProgressDetail(detail);
    }

    protected void recordTokenUsage(TaskExecutionContext context, int promptTokens, int completionTokens) {
        TaskExecutionContext.TokenUsage usage = new TaskExecutionContext.TokenUsage();
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);
        context.setTokenUsage(usage);
    }
}
