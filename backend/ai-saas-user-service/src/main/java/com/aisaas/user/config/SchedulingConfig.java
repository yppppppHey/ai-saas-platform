package com.aisaas.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务（VIP 过期检查 checkAndExpireVip）
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
