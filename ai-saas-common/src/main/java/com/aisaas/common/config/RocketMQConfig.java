package com.aisaas.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.producer.send-message-timeout:3000}")
    private Integer sendMessageTimeout;

    @Value("${rocketmq.producer.retry-times-when-send-failed:2}")
    private Integer retryTimesWhenSendFailed;

    @Value("${rocketmq.producer.retry-times-when-send-async-failed:2}")
    private Integer retryTimesWhenSendAsyncFailed;

    /**
     * 发送同步消息
     */
    public static SendResult sendSyncMessage(RocketMQTemplate rocketMQTemplate, String topic, Object payload) {
        return rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(payload)
                .setHeader("KEYS", UUID.randomUUID().toString())
                .setHeader("MSG_ID", UUID.randomUUID().toString())
                .build());
    }

    /**
     * 发送异步消息
     */
    public static void sendAsyncMessage(RocketMQTemplate rocketMQTemplate, String topic, Object payload, SendCallback sendCallback) {
        rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(payload)
                .setHeader("KEYS", UUID.randomUUID().toString())
                .setHeader("MSG_ID", UUID.randomUUID().toString())
                .build(), sendCallback);
    }

    /**
     * 发送延迟消息
     * delayLevel: 1-18 对应不同的延迟时间
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     */
    public static SendResult sendDelayMessage(RocketMQTemplate rocketMQTemplate, String topic, Object payload, int delayLevel) {
        org.springframework.messaging.Message<?> message = MessageBuilder.withPayload(payload)
                .setHeader("KEYS", UUID.randomUUID().toString())
                .setHeader("MSG_ID", UUID.randomUUID().toString())
                .build();
        return rocketMQTemplate.syncSendDelayTimeSeconds(topic, message, delayLevel * 1000);
    }

    /**
     * 发送顺序消息
     */
    public static SendResult sendOrderlyMessage(RocketMQTemplate rocketMQTemplate, String topic, Object payload, String hashKey) {
        return rocketMQTemplate.syncSendOrderly(topic, MessageBuilder.withPayload(payload)
                .setHeader("KEYS", UUID.randomUUID().toString())
                .setHeader("MSG_ID", UUID.randomUUID().toString())
                .build(), hashKey);
    }
}
