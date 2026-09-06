package com.aisaas.common.ai.agent.workflow;

import java.util.List;
import java.util.Optional;

/**
 * 工作流定义仓储接口
 *
 * <p>将“工作流定义从何处加载/保存”与执行引擎解耦。common 模块默认提供
 * {@link InMemoryWorkflowDefinitionRepository}（从 classpath 下的 JSON 文件加载），
 * 业务服务可自由提供一个基于数据库的实现来覆盖默认 Bean。</p>
 */
public interface WorkflowDefinitionRepository {

    /**
     * 根据定义ID获取工作流定义
     *
     * @param definitionId 工作流定义ID
     * @return 工作流定义，不存在时返回 {@code null}
     */
    WorkflowDefinition getById(String definitionId);

    /**
     * 获取所有工作流定义
     *
     * @return 工作流定义列表（不可为 null）
     */
    List<WorkflowDefinition> getAll();

    /**
     * 保存/更新一条工作流定义（实现类应保证幂等）
     *
     * @param definition 工作流定义
     */
    void save(WorkflowDefinition definition);

    /**
     * 根据定义ID删除工作流定义
     *
     * @param definitionId 工作流定义ID
     * @return 是否删除成功
     */
    boolean delete(String definitionId);

    /**
     * 是否存在指定定义ID
     *
     * @param definitionId 工作流定义ID
     * @return 是否存在
     */
    default boolean exists(String definitionId) {
        return getById(definitionId) != null;
    }

    /**
     * 便捷方法：根据ID获取定义，不存在时抛出 {@link IllegalArgumentException}
     *
     * @param definitionId 工作流定义ID
     * @return 工作流定义
     */
    default WorkflowDefinition requireById(String definitionId) {
        WorkflowDefinition definition = getById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Workflow definition not found: " + definitionId);
        }
        return definition;
    }

    /**
     * 便捷方法：根据ID获取定义，包装为 Optional
     *
     * @param definitionId 工作流定义ID
     * @return Optional 包装的工作流定义
     */
    default Optional<WorkflowDefinition> findById(String definitionId) {
        return Optional.ofNullable(getById(definitionId));
    }
}
