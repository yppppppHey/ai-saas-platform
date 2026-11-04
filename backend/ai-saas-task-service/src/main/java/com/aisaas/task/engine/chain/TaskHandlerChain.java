package com.aisaas.task.engine.chain;

import com.aisaas.task.engine.context.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class TaskHandlerChain {

    private final List<TaskHandler> handlers;

    @Autowired
    public TaskHandlerChain(List<TaskHandler> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(TaskHandler::getOrder))
                .toList();
    }

    public void execute(TaskExecutionContext context) {
        if (handlers.isEmpty()) {
            log.warn("没有可用的任务处理器");
            return;
        }

        try {
            executeHandler(0, context);
        } catch (Exception e) {
            log.error("任务处理器链执行失败: taskId={}, error={}",
                    context.getTask().getTaskId(), e.getMessage(), e);
            throw e;
        }
    }

    private void executeHandler(int index, TaskExecutionContext context) {
        if (index >= handlers.size()) {
            return;
        }

        TaskHandler handler = handlers.get(index);
        log.debug("执行任务处理器: taskId={}, handler={}, order={}",
                context.getTask().getTaskId(), handler.getClass().getSimpleName(), handler.getOrder());

        handler.handle(context, new TaskHandlerChain(handlers.subList(index + 1, handlers.size())) {
            @Override
            public void execute(TaskExecutionContext ctx) {
                executeHandler(index + 1, ctx);
            }
        });
    }

}
