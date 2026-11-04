package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.engine.processor.TaskProcessor;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PreProcessingHandler implements TaskHandler {

    @Override
    public void handle(TaskExecutionContext context, TaskHandlerChain chain) {
        TaskAsyncJob task = context.getTask();
        TaskProcessor processor = context.getProcessor();

        log.debug("开始任务预处理: taskId={}, processor={}", task.getTaskId(), processor.getClass().getSimpleName());

        try {
            processor.preProcess(context);
            log.debug("任务预处理完成: taskId={}", task.getTaskId());
            chain.execute(context);
        } catch (Exception e) {
            log.error("任务预处理失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("任务预处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
