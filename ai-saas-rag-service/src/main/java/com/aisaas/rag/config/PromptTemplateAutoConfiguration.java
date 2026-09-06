package com.aisaas.rag.config;

import com.aisaas.common.ai.prompt.PromptTemplate;
import com.aisaas.common.ai.prompt.PromptTemplateConfig;
import com.aisaas.common.ai.prompt.PromptTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提示词模板自动配置类
 * 负责从配置文件加载模板并注册到管理器中
 */
@Slf4j
@Configuration
public class PromptTemplateAutoConfiguration {

    @Autowired
    private PromptTemplateManager templateManager;

    @Autowired(required = false)
    private PromptTemplateConfig templateConfig;

    /**
     * 应用启动完成后加载配置文件中定义的模板
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadTemplatesFromConfig() {
        if (templateConfig == null || !templateConfig.isEnabled()) {
            log.info("Prompt template config is disabled or not available");
            return;
        }

        List<PromptTemplateConfig.TemplateDefinition> templates = templateConfig.getTemplates();
        if (templates == null || templates.isEmpty()) {
            log.info("No custom prompt templates defined in configuration");
            return;
        }

        int loadedCount = 0;
        for (PromptTemplateConfig.TemplateDefinition def : templates) {
            if (def.getId() == null || def.getTemplate() == null) {
                log.warn("Skipping invalid template definition: {}", def);
                continue;
            }

            PromptTemplate template = PromptTemplate.builder()
                    .id(def.getId())
                    .name(def.getName() != null ? def.getName() : def.getId())
                    .description(def.getDescription())
                    .sceneType(def.getSceneType())
                    .template(def.getTemplate())
                    .enabled(def.isEnabled())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            templateManager.registerTemplate(template);
            loadedCount++;
        }

        log.info("Successfully loaded {} prompt templates from configuration", loadedCount);
    }
}
