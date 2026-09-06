package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.engine.processor.TaskProcessor;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostProcessingHandler implements TaskHandler {

    @Override
    public void handle(TaskExecutionContext context, TaskHandlerChain chain) {
        TaskAsyncJob task = context.getTask();
        TaskProcessor processor = context.getProcessor();

        log.debug("开始任务后处理: taskId={}", task.getTaskId());

        try {
            processor.postProcess(context);
            log.debug("任务后处理完成: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.warn("任务后处理失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
