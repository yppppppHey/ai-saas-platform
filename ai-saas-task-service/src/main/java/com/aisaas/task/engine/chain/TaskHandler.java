package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;

public interface TaskHandler {

    void handle(TaskExecutionContext context, TaskHandlerChain chain);

    int getOrder();
}
