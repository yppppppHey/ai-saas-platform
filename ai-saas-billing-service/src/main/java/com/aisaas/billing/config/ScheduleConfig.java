package com.aisaas.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务（对账补偿 UsageReconcileTask）
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
