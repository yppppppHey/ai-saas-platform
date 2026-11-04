package com.aisaas.chat.service.impl;

import com.aisaas.chat.dto.template.PromptTemplateCreateDTO;
import com.aisaas.chat.dto.template.PromptTemplateDetailDTO;
import com.aisaas.chat.dto.template.PromptTemplateListDTO;
import com.aisaas.chat.dto.template.PromptTemplateUpdateDTO;
import com.aisaas.chat.entity.ChatPromptTemplate;
import com.aisaas.chat.mapper.ChatPromptTemplateMapper;
import com.aisaas.chat.service.ChatPromptTemplateService;
import com.aisaas.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 提示词模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPromptTemplateServiceImpl extends ServiceImpl<ChatPromptTemplateMapper, ChatPromptTemplate>
        implements ChatPromptTemplateService {

    private final ChatPromptTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    // 模板变量匹配正则: {{variableName}} 或 {{variableName:defaultValue}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}:]+)(?::([^}]*))?\\s*\\}\\}");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(Long userId, PromptTemplateCreateDTO dto) {
        // 验证名称唯一性
        checkNameUnique(dto.getName(), null);

        ChatPromptTemplate template = new ChatPromptTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setCreatorId(userId);
        template.setIsBuiltin(0); // 用户自定义模板
        template.setUsageCount(0);
        template.setStatus(1);

        // 处理变量定义
        if (dto.getVariables() != null) {
            try {
                template.setVariables(objectMapper.writeValueAsString(dto.getVariables()));
            } catch (Exception e) {
                log.error("序列化变量定义失败", e);
            }
        }

        templateMapper.insert(template);
        log.info("创建提示词模板成功: userId={}, templateId={}", userId, template.getId());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(Long userId, Long templateId, PromptTemplateUpdateDTO dto) {
        ChatPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException("模板不存在");
        }

        // 检查权限（只能修改自己的模板，不能修改内置模板）
        if (template.getIsBuiltin() != null && template.getIsBuiltin() == 1) {
            throw new BizException("不能修改内置模板");
        }
        if (!template.getCreatorId().equals(userId)) {
            throw new BizException("无权限修改此模板");
        }

        // 验证名称唯一性
        if (StringUtils.hasText(dto.getName()) && !dto.getName().equals(template.getName())) {
            checkNameUnique(dto.getName(), templateId);
        }

        // 更新字段
        if (StringUtils.hasText(dto.getName())) {
            template.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            template.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getContent())) {
            template.setContent(dto.getContent());
        }
        if (StringUtils.hasText(dto.getCategory())) {
            template.setCategory(dto.getCategory());
        }
        if (dto.getStatus() != null) {
            template.setStatus(dto.getStatus());
        }

        // 处理变量定义
        if (dto.getVariables() != null) {
            try {
                template.setVariables(objectMapper.writeValueAsString(dto.getVariables()));
            } catch (Exception e) {
                log.error("序列化变量定义失败", e);
            }
        }

        templateMapper.updateById(template);
        log.info("更新提示词模板成功: userId={}, templateId={}", userId, templateId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long userId, Long templateId) {
        ChatPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return true; // 已经不存在了
        }

        // 检查权限（只能删除自己的模板，不能删除内置模板）
        if (template.getIsBuiltin() != null && template.getIsBuiltin() == 1) {
            throw new BizException("不能删除内置模板");
        }
        if (!template.getCreatorId().equals(userId)) {
            throw new BizException("无权限删除此模板");
        }

        templateMapper.deleteById(templateId);
        log.info("删除提示词模板成功: userId={}, templateId={}", userId, templateId);
        return true;
    }

    @Override
    public PromptTemplateDetailDTO getTemplateDetail(Long templateId) {
        ChatPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return null;
        }

        PromptTemplateDetailDTO dto = new PromptTemplateDetailDTO();
        BeanUtils.copyProperties(template, dto);

        // 解析变量定义
        if (StringUtils.hasText(template.getVariables())) {
            try {
                Map<String, Object> variables = objectMapper.readValue(template.getVariables(),
                        new TypeReference<Map<String, Object>>() {});
                dto.setVariableDefinitions(variables);
            } catch (Exception e) {
                log.error("解析变量定义失败", e);
            }
        }

        // 解析扩展字段
        if (StringUtils.hasText(template.getExtras())) {
            try {
                Map<String, Object> extras = objectMapper.readValue(template.getExtras(),
                        new TypeReference<Map<String, Object>>() {});
                dto.setExtraParams(extras);
            } catch (Exception e) {
                log.error("解析扩展字段失败", e);
            }
        }

        return dto;
    }

    @Override
    public Page<PromptTemplateListDTO> listTemplates(String keyword, String category, String templateType,
                                                       Integer isBuiltin, Integer pageNum, Integer pageSize) {
        Page<ChatPromptTemplate> page = new Page<>(pageNum, pageSize);

        Page<ChatPromptTemplate> templatePage = templateMapper.selectTemplatePage(
                page, keyword, category, templateType, isBuiltin);

        Page<PromptTemplateListDTO> result = new Page<>(templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());
        result.setRecords(templatePage.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<PromptTemplateListDTO> listBuiltinTemplates(String category) {
        LambdaQueryWrapper<ChatPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPromptTemplate::getIsBuiltin, 1)
                .eq(ChatPromptTemplate::getStatus, 1)
                .eq(StringUtils.hasText(category), ChatPromptTemplate::getCategory, category)
                .orderByDesc(ChatPromptTemplate::getUsageCount);

        List<ChatPromptTemplate> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplateListDTO> listUserTemplates(Long userId) {
        LambdaQueryWrapper<ChatPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPromptTemplate::getCreatorId, userId)
                .eq(ChatPromptTemplate::getIsBuiltin, 0)
                .eq(ChatPromptTemplate::getIsDeleted, 0)
                .orderByDesc(ChatPromptTemplate::getCreatedAt);

        List<ChatPromptTemplate> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String renderTemplate(Long templateId, Map<String, Object> variables) {
        ChatPromptTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException("模板不存在");
        }
        return renderTemplateContent(template.getContent(), variables);
    }

    @Override
    public String renderTemplateContent(String templateContent, Map<String, Object> variables) {
        if (!StringUtils.hasText(templateContent)) {
            return "";
        }

        String result = templateContent;
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String defaultValue = matcher.group(2) != null ? matcher.group(2).trim() : "";

            Object value = variables != null && variables.containsKey(variableName)
                    ? variables.get(variableName)
                    : defaultValue;

            String replacement = value != null ? value.toString() : defaultValue;
            result = result.replace(matcher.group(0), Matcher.quoteReplacement(replacement));
        }

        return result;
    }

    @Override
    public void incrementUsageCount(Long templateId) {
        templateMapper.incrementUsageCount(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cloneTemplate(Long userId, Long templateId, String newName, String newCategory) {
        ChatPromptTemplate sourceTemplate = templateMapper.selectById(templateId);
        if (sourceTemplate == null) {
            throw new BizException("源模板不存在");
        }

        ChatPromptTemplate newTemplate = new ChatPromptTemplate();
        BeanUtils.copyProperties(sourceTemplate, newTemplate);
        newTemplate.setId(null);
        newTemplate.setName(StringUtils.hasText(newName) ? newName : sourceTemplate.getName() + " 副本");
        newTemplate.setCategory(StringUtils.hasText(newCategory) ? newCategory : sourceTemplate.getCategory());
        newTemplate.setCreatorId(userId);
        newTemplate.setIsBuiltin(0);
        newTemplate.setUsageCount(0);
        newTemplate.setCreatedAt(null);
        newTemplate.setUpdatedAt(null);
        newTemplate.setIsDeleted(0);

        templateMapper.insert(newTemplate);
        log.info("克隆模板成功: userId={}, sourceTemplateId={}, newTemplateId={}",
                userId, templateId, newTemplate.getId());
        return newTemplate.getId();
    }

    @Override
    public List<PromptTemplateListDTO> searchTemplates(String keyword, Integer limit) {
        LambdaQueryWrapper<ChatPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(ChatPromptTemplate::getName, keyword)
                        .or()
                        .like(ChatPromptTemplate::getDescription, keyword)
                        .or()
                        .like(ChatPromptTemplate::getContent, keyword))
                .eq(ChatPromptTemplate::getStatus, 1)
                .eq(ChatPromptTemplate::getIsDeleted, 0)
                .orderByDesc(ChatPromptTemplate::getUsageCount)
                .last(limit != null ? "LIMIT " + limit : "LIMIT 20");

        List<ChatPromptTemplate> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    private PromptTemplateListDTO convertToListDTO(ChatPromptTemplate template) {
        PromptTemplateListDTO dto = new PromptTemplateListDTO();
        BeanUtils.copyProperties(template, dto);

        // 解析变量定义，提取变量名列表
        if (StringUtils.hasText(template.getVariables())) {
            try {
                Map<String, Object> variables = objectMapper.readValue(template.getVariables(),
                        new TypeReference<Map<String, Object>>() {});
                dto.setVariableNames(new ArrayList<>(variables.keySet()));
            } catch (Exception e) {
                log.error("解析变量定义失败", e);
            }
        }

        return dto;
    }

    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<ChatPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatPromptTemplate::getName, name)
                .eq(ChatPromptTemplate::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(ChatPromptTemplate::getId, excludeId);
        }
        if (templateMapper.selectCount(wrapper) > 0) {
            throw new BizException("模板名称已存在: " + name);
        }
    }
}