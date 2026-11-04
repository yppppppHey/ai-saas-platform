package com.aisaas.common.ai.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool注册表
 * 管理和提供可用的Tool注册和发现功能
 */
@Slf4j
@Component
public class ToolRegistry {

    /**
     * Tool存储
     */
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * Tool分类存储
     */
    private final Map<String, Set<String>> categoryTools = new ConcurrentHashMap<>();

    /**
     * 注册Tool
     */
    public void register(Tool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("Tool and tool name cannot be null");
        }

        String name = tool.getName();
        tools.put(name, tool);

        // 从描述中推断分类
        String category = inferCategory(tool.getDescription());
        categoryTools.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(name);

        log.info("Tool registered: name={}, category={}", name, category);
    }

    /**
     * 批量注册Tools
     */
    public void registerAll(Collection<Tool> toolList) {
        if (toolList != null) {
            for (Tool tool : toolList) {
                register(tool);
            }
        }
    }

    /**
     * 注销Tool
     */
    public void unregister(String name) {
        Tool tool = tools.remove(name);
        if (tool != null) {
            // 从分类中移除
            for (Set<String> toolSet : categoryTools.values()) {
                toolSet.remove(name);
            }
            log.info("Tool unregistered: {}", name);
        }
    }

    /**
     * 获取Tool
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 检查是否存在指定Tool
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 获取所有已注册的Tools
     */
    public Collection<Tool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * 获取所有Tool名称
     */
    public Set<String> getAllToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * 根据分类获取Tools
     */
    public Set<Tool> getToolsByCategory(String category) {
        Set<String> toolNames = categoryTools.get(category);
        if (toolNames == null) {
            return Collections.emptySet();
        }

        Set<Tool> result = new HashSet<>();
        for (String name : toolNames) {
            Tool tool = tools.get(name);
            if (tool != null) {
                result.add(tool);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * 获取所有分类
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categoryTools.keySet());
    }

    /**
     * 搜索Tools
     */
    public List<Tool> searchTools(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return new ArrayList<>(tools.values());
        }

        String lowerKeyword = keyword.toLowerCase();
        List<Tool> result = new ArrayList<>();

        for (Tool tool : tools.values()) {
            if (tool.getName().toLowerCase().contains(lowerKeyword) ||
                    (tool.getDescription() != null && tool.getDescription().toLowerCase().contains(lowerKeyword))) {
                result.add(tool);
            }
        }

        return result;
    }

    /**
     * 清除所有Tools
     */
    public void clear() {
        tools.clear();
        categoryTools.clear();
        log.info("All tools cleared");
    }

    /**
     * 推断Tool分类
     */
    private String inferCategory(String description) {
        if (description == null) {
            return "general";
        }

        String lowerDesc = description.toLowerCase();

        if (lowerDesc.contains("search") || lowerDesc.contains("query") || lowerDesc.contains("find")) {
            return "search";
        }
        if (lowerDesc.contains("code") || lowerDesc.contains("program") || lowerDesc.contains("compile")) {
            return "code";
        }
        if (lowerDesc.contains("data") || lowerDesc.contains("analysis") || lowerDesc.contains("calculate")) {
            return "data";
        }
        if (lowerDesc.contains("file") || lowerDesc.contains("read") || lowerDesc.contains("write")) {
            return "file";
        }
        if (lowerDesc.contains("web") || lowerDesc.contains("http") || lowerDesc.contains("api")) {
            return "web";
        }

        return "general";
    }
}
