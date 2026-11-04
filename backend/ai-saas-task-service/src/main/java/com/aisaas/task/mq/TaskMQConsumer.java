package com.aisaas.task.mq;

import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.entity.TaskAsyncJob;
import com.aisaas.task.engine.TaskExecutionEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${mq.topic.task:ai-task-topic}",
        consumerGroup = "${mq.consumer.group.task:task-consumer-group}",
        selectorExpression = "*"
)
public class TaskMQConsumer implements RocketMQListener<TaskAsyncJob> {

    @Autowired
    private TaskExecutionEngine taskExecutionEngine;

    @Override
    public void onMessage(TaskAsyncJob task) {
        log.info("收到任务消息: taskId={}, taskType={}, userId={}",
                task.getTaskId(), task.getTaskType(), task.getUserId());

        try {
            taskExecutionEngine.executeTask(task);
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
        }
    }
}
