package com.aisaas.task.mq;

import com.aisaas.task.entity.TaskAsyncJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskMQProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${mq.topic.task:ai-task-topic}")
    private String taskTopic;

    public void sendTaskMessage(TaskAsyncJob task) {
        try {
            String destination = taskTopic + ":" + task.getTaskType();

            rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(task)
                    .setHeader("KEYS", task.getTaskId())
                    .setHeader("taskId", task.getTaskId())
                    .setHeader("userId", task.getUserId())
                    .build());

            log.info("任务消息发送成功: taskId={}, topic={}", task.getTaskId(), destination);
        } catch (Exception e) {
            log.error("任务消息发送失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("任务消息发送失败", e);
        }
    }

    public void sendDelayTaskMessage(TaskAsyncJob task, int delayLevel) {
        try {
            String destination = taskTopic + ":" + task.getTaskType();

            rocketMQTemplate.syncSendDelayTimeSeconds(destination, MessageBuilder.withPayload(task)
                    .setHeader("KEYS", task.getTaskId())
                    .setHeader("taskId", task.getTaskId())
                    .build(), delayLevel);

            log.info("延迟任务消息发送成功: taskId={}, delayLevel={}", task.getTaskId(), delayLevel);
        } catch (Exception e) {
            log.error("延迟任务消息发送失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
            throw new RuntimeException("延迟任务消息发送失败", e);
        }
    }
}
