package com.aisaas.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 动态路由配置
 * 支持从Nacos配置中心动态加载和刷新路由配置
 */
@Slf4j
@Component
public class DynamicRouteConfig implements ApplicationEventPublisherAware, ApplicationRunner {

    private static final String ROUTE_DATA_ID = "ai-saas-gateway-routes";
    private static final String ROUTE_GROUP = "DEFAULT_GROUP";
    private static final String DEFAULT_NAMESPACE = "";

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    private ObjectMapper objectMapper;

    private ApplicationEventPublisher publisher;

    private Listener configListener;

    private final Executor executor = Executors.newFixedThreadPool(2);

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 启动时加载路由配置
        loadRouteConfig();
    }

    /**
     * 初始化Nacos配置监听
     */
    @PostConstruct
    public void init() {
        try {
            configListener = new Listener() {
                @Override
                public Executor getExecutor() {
                    return executor;
                }

                @Override
                public void receiveConfigInfo(String config) {
                    log.info("Received route config change from Nacos");
                    refreshRoutes(config);
                }
            };

            // 添加配置监听
            nacosConfigManager.getConfigService().addListener(ROUTE_DATA_ID, ROUTE_GROUP, configListener);
            log.info("Nacos config listener initialized for route configuration");
        } catch (Exception e) {
            log.error("Failed to initialize Nacos config listener: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载路由配置
     */
    private void loadRouteConfig() {
        try {
            String config = nacosConfigManager.getConfigService()
                    .getConfig(ROUTE_DATA_ID, ROUTE_GROUP, 5000);
            
            if (StringUtils.isNotBlank(config)) {
                log.info("Loaded route configuration from Nacos");
                refreshRoutes(config);
            } else {
                log.warn("No route configuration found in Nacos");
            }
        } catch (Exception e) {
            log.error("Failed to load route configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * 刷新路由配置
     */
    private void refreshRoutes(String config) {
        try {
            List<RouteDefinition> routeDefinitions = parseRouteDefinitions(config);
            
            // 清空现有路由
            clearRoutes();
            
            // 添加新路由
            for (RouteDefinition definition : routeDefinitions) {
                routeDefinitionWriter.save(Mono.just(definition)).subscribe();
                log.debug("Route added: {}", definition.getId());
            }
            
            // 发布路由刷新事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            
            log.info("Routes refreshed successfully. Total routes: {}", routeDefinitions.size());
        } catch (Exception e) {
            log.error("Failed to refresh routes: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析路由定义
     */
    private List<RouteDefinition> parseRouteDefinitions(String config) throws JsonProcessingException {
        // 首先尝试解析为RouteDefinition列表
        try {
            return objectMapper.readValue(config, new TypeReference<List<RouteDefinition>>() {});
        } catch (JsonProcessingException e) {
            // 尝试解析为自定义格式
            RouteConfigWrapper wrapper = objectMapper.readValue(config, RouteConfigWrapper.class);
            return wrapper.getRoutes();
        }
    }

    /**
     * 清空所有路由
     */
    private void clearRoutes() {
        // 获取当前所有路由并删除
        routeDefinitionWriter.delete(Mono.just("*")).subscribe();
    }

    /**
     * 添加单个路由（供外部调用）
     */
    public void addRoute(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Route added: {}", definition.getId());
    }

    /**
     * 删除单个路由（供外部调用）
     */
    public void deleteRoute(String routeId) {
        routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("Route deleted: {}", routeId);
    }

    /**
     * 路由配置包装类
     */
    @Data
    public static class RouteConfigWrapper {
        private List<RouteDefinition> routes;
    }

    @PreDestroy
    public void destroy() {
        try {
            if (configListener != null) {
                nacosConfigManager.getConfigService().removeListener(ROUTE_DATA_ID, ROUTE_GROUP, configListener);
                log.info("Nacos config listener removed");
            }
        } catch (Exception e) {
            log.error("Error removing Nacos config listener: {}", e.getMessage());
        }
    }
}
