package com.aisaas.task.engine.context;

import com.aisaas.task.engine.processor.TaskProcessor;
import com.aisaas.task.entity.TaskAsyncJob;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TaskExecutionContext {

    private final TaskAsyncJob task;

    private final TaskProcessor processor;

    private Object outputResult;

    private Map<String, Object> executionData = new HashMap<>();

    private Long startTime;

    private Long endTime;

    private TokenUsage tokenUsage;

    private java.math.BigDecimal costUsd;

    public TaskExecutionContext(TaskAsyncJob task, TaskProcessor processor) {
        this.task = task;
        this.processor = processor;
        this.startTime = System.currentTimeMillis();
    }

    public void putData(String key, Object value) {
        executionData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) executionData.get(key);
    }

    public void finish() {
        this.endTime = System.currentTimeMillis();
    }

    public long getDuration() {
        if (endTime != null && startTime != null) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    @Data
    public static class TokenUsage {
        private Integer promptTokens = 0;
        private Integer completionTokens = 0;
        private Integer totalTokens = 0;
    }
}
