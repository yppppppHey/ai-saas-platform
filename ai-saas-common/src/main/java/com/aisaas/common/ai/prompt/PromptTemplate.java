package com.aisaas.common.ai.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 提示词模板
 * 用于管理和渲染提示词模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    /**
     * 模板ID
     */
    private String id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板内容
     */
    private String template;

    /**
     * 场景类型（如：rag_qa, summarization, translation等）
     */
    private String sceneType;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 渲染模板
     *
     * @param variables 变量映射
     * @return 渲染后的提示词
     */
    public String render(Map<String, Object> variables) {
        if (template == null) {
            return "";
        }

        String result = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }

    /**
     * 渲染模板（使用可变参数）
     *
     * @param keyValuePairs 键值对（key1, value1, key2, value2...）
     * @return 渲染后的提示词
     */
    public String render(Object... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length % 2 != 0) {
            return render(java.util.Collections.emptyMap());
        }

        Map<String, Object> variables = new java.util.HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (keyValuePairs[i] != null) {
                variables.put(keyValuePairs[i].toString(), keyValuePairs[i + 1]);
            }
        }

        return render(variables);
    }
}
