package com.aisaas.gateway.controller;

import com.aisaas.common.result.Result;
import com.aisaas.gateway.service.IpBlacklistService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * IP黑白名单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/gateway/ip")
@RequiredArgsConstructor
public class IpBlacklistController {

    private final IpBlacklistService ipBlacklistService;

    /**
     * 获取IP黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<Set<String>> getBlacklist() {
        Set<String> blacklist = ipBlacklistService.getBlacklist();
        return Result.success(blacklist);
    }

    /**
     * 添加IP到黑名单
     */
    @PostMapping("/blacklist")
    public Result<Void> addToBlacklist(@RequestBody IpOperationRequest request) {
        log.info("Adding IPs to blacklist: {}", request.getIps());
        ipBlacklistService.addToBlacklist(request.getIps());
        return Result.success();
    }

    /**
     * 从黑名单移除IP
     */
    @DeleteMapping("/blacklist")
    public Result<Void> removeFromBlacklist(@RequestBody IpOperationRequest request) {
        log.info("Removing IPs from blacklist: {}", request.getIps());
        ipBlacklistService.removeFromBlacklist(request.getIps());
        return Result.success();
    }

    /**
     * 获取IP白名单列表
     */
    @GetMapping("/whitelist")
    public Result<Set<String>> getWhitelist() {
        Set<String> whitelist = ipBlacklistService.getWhitelist();
        return Result.success(whitelist);
    }

    /**
     * 添加IP到白名单
     */
    @PostMapping("/whitelist")
    public Result<Void> addToWhitelist(@RequestBody IpOperationRequest request) {
        log.info("Adding IPs to whitelist: {}", request.getIps());
        ipBlacklistService.addToWhitelist(request.getIps());
        return Result.success();
    }

    /**
     * 从白名单移除IP
     */
    @DeleteMapping("/whitelist")
    public Result<Void> removeFromWhitelist(@RequestBody IpOperationRequest request) {
        log.info("Removing IPs from whitelist: {}", request.getIps());
        ipBlacklistService.removeFromWhitelist(request.getIps());
        return Result.success();
    }

    /**
     * 临时封禁IP
     */
    @PostMapping("/block")
    public Result<Void> blockIp(@RequestBody BlockIpRequest request) {
        log.info("Blocking IP: {} for {} minutes", request.getIp(), request.getDurationMinutes());
        ipBlacklistService.blockIp(request.getIp(), request.getDurationMinutes());
        return Result.success();
    }

    /**
     * 检查IP状态
     */
    @GetMapping("/status")
    public Result<IpStatusResponse> checkIpStatus(@RequestParam String ip) {
        IpStatusResponse response = new IpStatusResponse();
        response.setIp(ip);
        response.setBlacklisted(ipBlacklistService.isInBlacklist(ip));
        response.setWhitelisted(ipBlacklistService.isInWhitelist(ip));
        response.setBlocked(ipBlacklistService.isBlocked(ip));
        return Result.success(response);
    }
}

/**
 * IP操作请求
 */
@Data
class IpOperationRequest {
    private List<String> ips;
}

/**
 * 封禁IP请求
 */
@Data
class BlockIpRequest {
    private String ip;
    private Integer durationMinutes = 60; // 默认封禁1小时
}

/**
 * IP状态响应
 */
@Data
class IpStatusResponse {
    private String ip;
    private boolean blacklisted;
    private boolean whitelisted;
    private boolean blocked;
}
