package com.aisaas.common.ai.agent.node;

import com.aisaas.common.ai.agent.node.executor.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器工厂
 * 管理和提供各种类型的节点执行器
 */
@Slf4j
@Component
public class NodeExecutorFactory {

    /**
     * 执行器注册表
     */
    private final Map<String, NodeExecutor> executors = new HashMap<>();

    /**
     * 注入所有节点执行器
     */
    @Autowired
    public void setExecutors(List<NodeExecutor> executorList) {
        for (NodeExecutor executor : executorList) {
            registerExecutor(executor);
        }
    }

    /**
     * 注册执行器
     */
    public void registerExecutor(NodeExecutor executor) {
        String nodeType = executor.getNodeType();
        executors.put(nodeType, executor);
        log.info("Registered node executor: {}", nodeType);
    }

    /**
     * 获取执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        return executors.get(nodeType);
    }

    /**
     * 检查是否支持该节点类型
     */
    public boolean supports(String nodeType) {
        return executors.containsKey(nodeType);
    }

    /**
     * 获取所有支持的节点类型
     */
    public java.util.Set<String> getSupportedTypes() {
        return executors.keySet();
    }
}
