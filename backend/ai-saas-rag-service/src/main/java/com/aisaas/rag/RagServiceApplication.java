package com.aisaas.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * RAG知识库服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aisaas.rag", "com.aisaas.common"})
@EnableDiscoveryClient
public class RagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
