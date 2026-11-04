package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;
import com.aisaas.task.engine.processor.TaskProcessor;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExecutionHandler implements TaskHandler {

    @Override
    public void handle(TaskExecutionContext context, TaskHandlerChain chain) {
        TaskAsyncJob task = context.getTask();
        TaskProcessor processor = context.getProcessor();

        log.info("开始执行任务: taskId={}, taskType={}", task.getTaskId(), task.getTaskType());

        try {
            Object result = processor.execute(context);
            context.setOutputResult(result);

            log.info("任务执行完成: taskId={}, result={}", task.getTaskId(), result != null ? "有结果" : "无结果");

            if (chain != null) {
                chain.execute(context);
            }
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("任务执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
