package com.aisaas.chat.entity;

import com.aisaas.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提示词模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_prompt_template")
public class ChatPromptTemplate extends BaseEntity {

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
     * 变量定义(JSON)
     */
    private String variables;

    /**
     * 模板类型: system/user/assistant
     */
    @TableField("template_type")
    private String templateType;

    /**
     * 场景分类
     */
    private String category;

    /**
     * 是否为内置模板: 0-否 1-是
     */
    @TableField("is_builtin")
    private Integer isBuiltin;

    /**
     * 创建者ID(0表示系统)
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 使用次数
     */
    @TableField("usage_count")
    private Integer usageCount;

    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 扩展字段(JSON)
     */
    private String extras;
}
