package com.aisaas.common.ai.prompt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词模板配置属性
 * 支持从application.yml加载模板配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.prompt-template")
public class PromptTemplateConfig {

    /**
     * 是否启用自定义模板
     */
    private boolean enabled = true;

    /**
     * 自定义模板列表
     */
    private List<TemplateDefinition> templates = new ArrayList<>();

    /**
     * 模板定义
     */
    @Data
    public static class TemplateDefinition {
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
         * 场景类型
         */
        private String sceneType;

        /**
         * 模板内容
         */
        private String template;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }
}
