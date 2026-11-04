package com.aisaas.chat.dto.template;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 提示词模板详情DTO
 */
@Data
public class PromptTemplateDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private Long id;

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
    private Map<String, Object> variableDefinitions;

    /**
     * 变量名列表
     */
    private List<String> variableNames;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 场景分类
     */
    private String category;

    /**
     * 是否为内置模板
     */
    private Integer isBuiltin;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}