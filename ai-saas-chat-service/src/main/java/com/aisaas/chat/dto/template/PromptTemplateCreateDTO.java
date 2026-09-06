package com.aisaas.chat.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 提示词模板创建DTO
 */
@Data
public class PromptTemplateCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板内容
     */
    @NotBlank(message = "模板内容不能为空")
    private String content;

    /**
     * 变量定义（变量名->变量描述/默认值）
     */
    private Map<String, Object> variables;

    /**
     * 模板类型: system/user/assistant
     */
    private String templateType = "system";

    /**
     * 场景分类
     */
    private String category;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status = 1;
}