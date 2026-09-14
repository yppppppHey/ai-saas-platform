package com.aisaas.task.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.aisaas.task.mapper")
public class TaskMybatisPlusConfig {

    // mybatisPlusInterceptor 由 common 的 MyBatisPlusConfig 提供（@ConditionalOnMissingBean 兜底，
    // 含分页+乐观锁插件）；此处仅保留 @MapperScan，避免与 common 的同名 bean 冲突
}
