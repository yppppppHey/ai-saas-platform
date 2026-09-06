package com.aisaas.chat.controller;

import com.aisaas.chat.dto.template.*;
import com.aisaas.chat.service.ChatPromptTemplateService;
import com.aisaas.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板控制器
 * 提供模板CRUD、变量替换、内置模板管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prompt-templates")
@RequiredArgsConstructor
public class ChatPromptTemplateController {

    private final ChatPromptTemplateService templateService;

    /**
     * 创建自定义模板
     */
    @PostMapping
    public Result<Long> createTemplate(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody PromptTemplateCreateDTO dto) {
        log.info("创建提示词模板: userId={}, name={}", userId, dto.getName());
        Long templateId = templateService.createTemplate(userId, dto);
        return Result.success(templateId);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{templateId}")
    public Result<Boolean> updateTemplate(@RequestAttribute("userId") Long userId,
                                          @PathVariable Long templateId,
                                          @Valid @RequestBody PromptTemplateUpdateDTO dto) {
        log.info("更新提示词模板: userId={}, templateId={}", userId, templateId);
        boolean success = templateService.updateTemplate(userId, templateId, dto);
        return Result.success(success);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{templateId}")
    public Result<Boolean> deleteTemplate(@RequestAttribute("userId") Long userId,
                                          @PathVariable Long templateId) {
        log.info("删除提示词模板: userId={}, templateId={}", userId, templateId);
        boolean success = templateService.deleteTemplate(userId, templateId);
        return Result.success(success);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{templateId}")
    public Result<PromptTemplateDetailDTO> getTemplateDetail(@PathVariable Long templateId) {
        log.info("获取模板详情: templateId={}", templateId);
        PromptTemplateDetailDTO detail = templateService.getTemplateDetail(templateId);
        return Result.success(detail);
    }

    /**
     * 分页查询模板列表
     */
    @GetMapping
    public Result<Page<PromptTemplateListDTO>> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Integer isBuiltin,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<PromptTemplateListDTO> page = templateService.listTemplates(
                keyword, category, templateType, isBuiltin, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取内置模板列表
     */
    @GetMapping("/builtin")
    public Result<List<PromptTemplateListDTO>> listBuiltinTemplates(
            @RequestParam(required = false) String category) {
        List<PromptTemplateListDTO> templates = templateService.listBuiltinTemplates(category);
        return Result.success(templates);
    }

    /**
     * 获取用户的自定义模板列表
     */
    @GetMapping("/my")
    public Result<List<PromptTemplateListDTO>> listUserTemplates(@RequestAttribute("userId") Long userId) {
        List<PromptTemplateListDTO> templates = templateService.listUserTemplates(userId);
        return Result.success(templates);
    }

    /**
     * 渲染模板（变量替换）
     */
    @PostMapping("/{templateId}/render")
    public Result<String> renderTemplate(@PathVariable Long templateId,
                                         @RequestBody(required = false) Map<String, Object> variables) {
        String rendered = templateService.renderTemplate(templateId, variables);
        return Result.success(rendered);
    }

    /**
     * 渲染模板内容（直接传入模板内容）
     */
    @PostMapping("/render-content")
    public Result<String> renderTemplateContent(@RequestParam String content,
                                                @RequestBody(required = false) Map<String, Object> variables) {
        String rendered = templateService.renderTemplateContent(content, variables);
        return Result.success(rendered);
    }

    /**
     * 克隆模板
     */
    @PostMapping("/{templateId}/clone")
    public Result<Long> cloneTemplate(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long templateId,
                                      @RequestParam(required = false) String newName,
                                      @RequestParam(required = false) String newCategory) {
        Long newTemplateId = templateService.cloneTemplate(userId, templateId, newName, newCategory);
        return Result.success(newTemplateId);
    }

    /**
     * 搜索模板
     */
    @GetMapping("/search")
    public Result<List<PromptTemplateListDTO>> searchTemplates(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<PromptTemplateListDTO> templates = templateService.searchTemplates(keyword, limit);
        return Result.success(templates);
    }
}