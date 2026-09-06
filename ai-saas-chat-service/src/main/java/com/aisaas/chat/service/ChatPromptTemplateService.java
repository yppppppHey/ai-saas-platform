package com.aisaas.chat.service;

import com.aisaas.chat.dto.template.PromptTemplateCreateDTO;
import com.aisaas.chat.dto.template.PromptTemplateDetailDTO;
import com.aisaas.chat.dto.template.PromptTemplateListDTO;
import com.aisaas.chat.dto.template.PromptTemplateUpdateDTO;
import com.aisaas.chat.entity.ChatPromptTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板服务接口
 */
public interface ChatPromptTemplateService extends IService<ChatPromptTemplate> {

    /**
     * 创建自定义模板
     *
     * @param userId 用户ID
     * @param dto    创建DTO
     * @return 创建的模板ID
     */
    Long createTemplate(Long userId, PromptTemplateCreateDTO dto);

    /**
     * 更新模板
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     * @param dto        更新DTO
     * @return 是否成功
     */
    boolean updateTemplate(Long userId, Long templateId, PromptTemplateUpdateDTO dto);

    /**
     * 删除模板
     *
     * @param userId     用户ID
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean deleteTemplate(Long userId, Long templateId);

    /**
     * 获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    PromptTemplateDetailDTO getTemplateDetail(Long templateId);

    /**
     * 分页查询模板列表
     *
     * @param keyword      关键词
     * @param category     分类
     * @param templateType 模板类型
     * @param isBuiltin    是否内置
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    Page<PromptTemplateListDTO> listTemplates(String keyword, String category, String templateType,
                                               Integer isBuiltin, Integer pageNum, Integer pageSize);

    /**
     * 获取内置模板列表
     *
     * @param category 分类（可选）
     * @return 模板列表
     */
    List<PromptTemplateListDTO> listBuiltinTemplates(String category);

    /**
     * 获取用户的自定义模板列表
     *
     * @param userId 用户ID
     * @return 模板列表
     */
    List<PromptTemplateListDTO> listUserTemplates(Long userId);

    /**
     * 渲染模板（变量替换）
     *
     * @param templateId 模板ID
     * @param variables  变量值
     * @return 渲染后的内容
     */
    String renderTemplate(Long templateId, Map<String, Object> variables);

    /**
     * 渲染模板（直接传入模板内容）
     *
     * @param templateContent 模板内容
     * @param variables       变量值
     * @return 渲染后的内容
     */
    String renderTemplateContent(String templateContent, Map<String, Object> variables);

    /**
     * 增加模板使用次数
     *
     * @param templateId 模板ID
     */
    void incrementUsageCount(Long templateId);

    /**
     * 克隆模板
     *
     * @param userId       用户ID
     * @param templateId   源模板ID
     * @param newName      新模板名称（可选）
     * @param newCategory 新分类（可选）
     * @return 新模板ID
     */
    Long cloneTemplate(Long userId, Long templateId, String newName, String newCategory);

    /**
     * 搜索模板
     *
     * @param keyword 关键词
     * @param limit   限制数量
     * @return 模板列表
     */
    List<PromptTemplateListDTO> searchTemplates(String keyword, Integer limit);
}