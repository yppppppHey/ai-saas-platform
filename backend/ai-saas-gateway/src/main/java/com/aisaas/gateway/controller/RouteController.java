package com.aisaas.gateway.controller;

import com.aisaas.common.result.Result;
import com.aisaas.gateway.config.DynamicRouteConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由管理控制器
 * 提供动态路由的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/gateway/routes")
@RequiredArgsConstructor
public class RouteController {

    private final DynamicRouteConfig dynamicRouteConfig;

    /**
     * 添加路由
     */
    @PostMapping
    public Result<Void> addRoute(@RequestBody RouteDefinition routeDefinition) {
        log.info("Adding route: {}", routeDefinition.getId());
        dynamicRouteConfig.addRoute(routeDefinition);
        return Result.success();
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{routeId}")
    public Result<Void> deleteRoute(@PathVariable String routeId) {
        log.info("Deleting route: {}", routeId);
        dynamicRouteConfig.deleteRoute(routeId);
        return Result.success();
    }

    /**
     * 更新路由
     */
    @PutMapping("/{routeId}")
    public Result<Void> updateRoute(@PathVariable String routeId, @RequestBody RouteDefinition routeDefinition) {
        log.info("Updating route: {}", routeId);
        dynamicRouteConfig.deleteRoute(routeId);
        routeDefinition.setId(routeId);
        dynamicRouteConfig.addRoute(routeDefinition);
        return Result.success();
    }

    /**
     * 获取网关健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return Result.success(health);
    }
}

/**
 * 路由信息DTO
 */
@Data
class RouteInfoDTO {
    private String id;
    private String uri;
    private List<String> predicates;
    private List<String> filters;
    private int order;
}

/**
 * 添加路由请求
 */
@Data
class AddRouteRequest {
    private RouteDefinition routeDefinition;
}

/**
 * 路由操作响应
 */
@Data
class RouteOperationResponse {
    private boolean success;
    private String message;
    private String routeId;
}
