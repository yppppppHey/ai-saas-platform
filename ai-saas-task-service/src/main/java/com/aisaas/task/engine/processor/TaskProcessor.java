package com.aisaas.task.engine.processor;

import com.aisaas.task.engine.context.TaskExecutionContext;

public interface TaskProcessor {

    String getTaskType();

    void preProcess(TaskExecutionContext context) throws Exception;

    Object execute(TaskExecutionContext context) throws Exception;

    void postProcess(TaskExecutionContext context) throws Exception;

    boolean supports(String taskType);
}
