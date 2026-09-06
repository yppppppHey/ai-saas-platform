package com.aisaas.common.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 提示词模板管理器
 * 负责管理提示词模板的增删改查和渲染
 */
@Slf4j
@Component
public class PromptTemplateManager {

    /**
     * 模板存储
     */
    private final Map<String, PromptTemplate> templateStore = new ConcurrentHashMap<>();

    /**
     * 初始化默认模板
     */
    @PostConstruct
    public void init() {
        log.info("Initializing prompt template manager");

        // 注册RAG问答默认模板
        registerTemplate(PromptTemplate.builder()
                .id("rag_qa_default")
                .name("RAG问答默认模板")
                .description("用于RAG问答场景的默认提示词模板")
                .sceneType("rag_qa")
                .template("你是一个专业的助手。请根据以下上下文信息回答用户的问题。\n\n" +
                        "上下文信息：\n{{context}}\n\n" +
                        "用户问题：{{query}}\n\n" +
                        "请根据上下文信息提供准确、简洁的回答。如果上下文信息不足以回答问题，请明确说明。")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        // 注册文档摘要默认模板
        registerTemplate(PromptTemplate.builder()
                .id("summarization_default")
                .name("文档摘要默认模板")
                .description("用于文档摘要场景的默认提示词模板")
                .sceneType("summarization")
                .template("请对以下文档内容进行摘要总结：\n\n{{content}}\n\n" +
                        "请提供：\n1. 主要内容概述\n2. 关键要点\n3. 重要结论")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        // 注册文本翻译默认模板
        registerTemplate(PromptTemplate.builder()
                .id("translation_default")
                .name("文本翻译默认模板")
                .description("用于文本翻译场景的默认提示词模板")
                .sceneType("translation")
                .template("请将以下文本翻译成{{targetLanguage}}：\n\n{{text}}\n\n" +
                        "翻译要求：\n1. 保持原文的语义准确\n2. 符合目标语言的表达习惯\n3. 保持专业术语的准确性")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        // 注册代码解释默认模板
        registerTemplate(PromptTemplate.builder()
                .id("code_explanation_default")
                .name("代码解释默认模板")
                .description("用于代码解释场景的默认提示词模板")
                .sceneType("code_explanation")
                .template("请解释以下代码：\n\n```{{language}}\n{{code}}\n```\n\n" +
                        "请提供：\n1. 代码功能概述\n2. 关键逻辑解释\n3. 输入输出说明\n4. 使用示例（如有必要）")
                .enabled(true)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        log.info("Registered {} default prompt templates", templateStore.size());
    }

    /**
     * 注册模板
     *
     * @param template 模板对象
     * @return 是否注册成功
     */
    public boolean registerTemplate(PromptTemplate template) {
        if (template == null || template.getId() == null) {
            log.warn("Cannot register null template or template without id");
            return false;
        }

        template.setUpdateTime(LocalDateTime.now());
        if (template.getCreateTime() == null) {
            template.setCreateTime(LocalDateTime.now());
        }

        templateStore.put(template.getId(), template);
        log.info("Registered prompt template: {} - {}", template.getId(), template.getName());
        return true;
    }

    /**
     * 根据ID获取模板
     *
     * @param templateId 模板ID
     * @return 模板对象，如果不存在则返回null
     */
    public PromptTemplate getTemplate(String templateId) {
        return templateStore.get(templateId);
    }

    /**
     * 根据场景类型获取默认模板
     *
     * @param sceneType 场景类型
     * @return 默认模板，如果不存在则返回null
     */
    public PromptTemplate getDefaultTemplateBySceneType(String sceneType) {
        return templateStore.values().stream()
                .filter(t -> sceneType.equals(t.getSceneType()) && t.isEnabled())
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有模板
     *
     * @return 模板列表
     */
    public List<PromptTemplate> getAllTemplates() {
        return new ArrayList<>(templateStore.values());
    }

    /**
     * 根据场景类型获取所有模板
     *
     * @param sceneType 场景类型
     * @return 模板列表
     */
    public List<PromptTemplate> getTemplatesBySceneType(String sceneType) {
        return templateStore.values().stream()
                .filter(t -> sceneType.equals(t.getSceneType()))
                .collect(Collectors.toList());
    }

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     * @return 是否删除成功
     */
    public boolean deleteTemplate(String templateId) {
        if (templateId == null) {
            return false;
        }
        PromptTemplate removed = templateStore.remove(templateId);
        if (removed != null) {
            log.info("Deleted prompt template: {} - {}", templateId, removed.getName());
            return true;
        }
        return false;
    }

    /**
     * 更新模板
     *
     * @param template 模板对象
     * @return 是否更新成功
     */
    public boolean updateTemplate(PromptTemplate template) {
        if (template == null || template.getId() == null) {
            return false;
        }

        if (!templateStore.containsKey(template.getId())) {
            log.warn("Template not found for update: {}", template.getId());
            return false;
        }

        template.setUpdateTime(LocalDateTime.now());
        templateStore.put(template.getId(), template);
        log.info("Updated prompt template: {} - {}", template.getId(), template.getName());
        return true;
    }

    /**
     * 渲染模板
     *
     * @param templateId 模板ID
     * @param variables 变量映射
     * @return 渲染后的提示词，如果模板不存在则返回null
     */
    public String renderTemplate(String templateId, Map<String, Object> variables) {
        PromptTemplate template = templateStore.get(templateId);
        if (template == null || !template.isEnabled()) {
            log.warn("Template not found or disabled: {}", templateId);
            return null;
        }
        return template.render(variables);
    }

    /**
     * 渲染模板（使用场景类型获取默认模板）
     *
     * @param sceneType 场景类型
     * @param variables 变量映射
     * @return 渲染后的提示词，如果模板不存在则返回null
     */
    public String renderTemplateBySceneType(String sceneType, Map<String, Object> variables) {
        PromptTemplate template = getDefaultTemplateBySceneType(sceneType);
        if (template == null) {
            log.warn("No default template found for scene type: {}", sceneType);
            return null;
        }
        return template.render(variables);
    }

    /**
     * 检查模板是否存在
     *
     * @param templateId 模板ID
     * @return 是否存在
     */
    public boolean exists(String templateId) {
        return templateStore.containsKey(templateId);
    }

    /**
     * 获取启用的模板数量
     *
     * @return 模板数量
     */
    public int getEnabledTemplateCount() {
        return (int) templateStore.values().stream()
                .filter(PromptTemplate::isEnabled)
                .count();
    }
}
