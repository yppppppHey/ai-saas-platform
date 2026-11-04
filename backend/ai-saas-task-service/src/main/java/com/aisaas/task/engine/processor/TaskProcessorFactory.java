package com.aisaas.task.engine.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TaskProcessorFactory {

    private final Map<String, TaskProcessor> processors = new ConcurrentHashMap<>();

    @Autowired
    public TaskProcessorFactory(List<TaskProcessor> processorList) {
        for (TaskProcessor processor : processorList) {
            String taskType = processor.getTaskType();
            if (processors.containsKey(taskType)) {
                log.warn("任务处理器重复注册: taskType={}", taskType);
            }
            processors.put(taskType, processor);
            log.info("注册任务处理器: taskType={}, processor={}", taskType, processor.getClass().getSimpleName());
        }
    }

    public TaskProcessor getProcessor(String taskType) {
        TaskProcessor processor = processors.get(taskType);
        if (processor == null) {
            log.error("未找到任务处理器: taskType={}", taskType);
        }
        return processor;
    }

    public boolean hasProcessor(String taskType) {
        return processors.containsKey(taskType);
    }

    public List<String> getSupportedTaskTypes() {
        return processors.keySet().stream().sorted().toList();
    }
}
