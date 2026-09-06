package com.aisaas.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * SpringDoc OpenAPI 配置
 * 实现接口文档 (SpringDoc OpenAPI) 的自动配置
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:ai-saas-service}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Value("${openapi.title:AI SaaS Platform API}")
    private String apiTitle;

    @Value("${openapi.description:AI SaaS Platform RESTful API Documentation}")
    private String apiDescription;

    @Value("${openapi.version:1.0.0}")
    private String apiVersion;

    @Bean
    public OpenAPI openAPI() {
        log.info("Initializing OpenAPI configuration for: {}", applicationName);

        // API 信息
        Info info = new Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion)
                .contact(new Contact()
                        .name("AI SaaS Team")
                        .email("team@aisaas.com")
                        .url("https://www.aisaas.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));

        // 服务器配置
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        Server devServer = new Server()
                .url("https://dev-api.aisaas.com")
                .description("Development Server");

        Server prodServer = new Server()
                .url("https://api.aisaas.com")
                .description("Production Server");

        // 安全方案
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Please enter JWT token");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        // 构建 OpenAPI
        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .servers(Arrays.asList(localServer, devServer, prodServer))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .security(Arrays.asList(securityRequirement));

        log.info("OpenAPI configuration completed for: {}", applicationName);

        return openAPI;
    }
}
