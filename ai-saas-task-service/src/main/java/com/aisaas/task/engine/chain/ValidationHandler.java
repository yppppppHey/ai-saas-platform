package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class ValidationHandler implements TaskHandler {

    @Override
    public void handle(TaskExecutionContext context, TaskHandlerChain chain) {
        TaskAsyncJob task = context.getTask();
        log.debug("开始任务验证: taskId={}", task.getTaskId());

        if (task.getInputParams() == null || task.getInputParams().isEmpty()) {
            throw new IllegalArgumentException("任务输入参数不能为空");
        }

        if (task.getPriority() == null || task.getPriority() < 1 || task.getPriority() > 10) {
            task.setPriority(5);
        }

        log.debug("任务验证通过: taskId={}", task.getTaskId());
        chain.execute(context);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
