package com.aisaas.common.ai.agent.template;

import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent模板
 * 预定义的Agent工作流模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 模板分类
     */
    private String category;

    /**
     * 版本
     */
    private String version;

    /**
     * 关联的工作流定义
     */
    private WorkflowDefinition workflowDefinition;

    /**
     * 工作流定义ID（如果没有直接关联定义）
     */
    private String workflowDefinitionId;

    /**
     * 输入参数定义
     */
    @Builder.Default
    private Map<String, ParameterDef> inputParameters = new HashMap<>();

    /**
     * 输出定义
     */
    @Builder.Default
    private Map<String, OutputDef> outputs = new HashMap<>();

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户提示词模板
     */
    private String userPromptTemplate;

    /**
     * 示例输入
     */
    private List<Example> examples;

    /**
     * 需要的Tools
     */
    private List<String> requiredTools;

    /**
     * 配置项
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 状态：DRAFT, PUBLISHED, DEPRECATED
     */
    @Builder.Default
    private String status = "DRAFT";

    // ============ 内部类定义 ============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParameterDef {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
        private List<String> enumValues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutputDef {
        private String name;
        private String type;
        private String description;
        private String mapping;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Example {
        private String name;
        private String description;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
    }
}
