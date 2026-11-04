package com.aisaas.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池集中配置
 *
 * 为什么不用 Executors.newFixedThreadPool / newCachedThreadPool：
 * 1. 队列无界(newCachedThreadPool 甚至可无限建线程) -> 有 OOM 风险
 * 2. 默认线程工厂线程名无意义 -> 出问题无法定位
 * 3. 不由 Spring 管理 -> 无法随容器优雅关闭, 任务会被硬中断
 *
 * 这里统一：有界队列 + 命名线程 + CallerRunsPolicy 兜底 + destroyMethod 优雅关闭。
 */
@Configuration
public class GatewayExecutorConfig {

    /** 动态路由刷新线程池 */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService routeRefreshExecutor() {
        AtomicInteger idx = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "route-refresh-" + idx.incrementAndGet());
            t.setDaemon(false);
            return t;
        };
        return new ThreadPoolExecutor(
                2, 4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
