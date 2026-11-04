package com.aisaas.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Chat Service 启动类
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aisaas.chat.client")
@SpringBootApplication(scanBasePackages = {"com.aisaas.chat", "com.aisaas.common"})
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
