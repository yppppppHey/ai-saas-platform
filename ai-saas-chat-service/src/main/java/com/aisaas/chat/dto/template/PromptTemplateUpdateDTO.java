package com.aisaas.chat.dto.template;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 提示词模板更新DTO
 */
@Data
public class PromptTemplateUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private String content;

    /**
     * 变量定义
     */
    private Map<String, Object> variables;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 场景分类
     */
    private String category;

    /**
     * 状态
     */
    private Integer status;
}