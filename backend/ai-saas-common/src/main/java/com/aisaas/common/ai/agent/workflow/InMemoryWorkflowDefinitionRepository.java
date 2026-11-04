package com.aisaas.common.ai.agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存 + classpath JSON 资源的工作流定义仓储实现。
 *
 * <p>启动时会扫描所有 classpath 下的 {@code /workflows/*.json} 文件并反序列化为
 * {@link WorkflowDefinition}；同时提供运行时 {@code save}/{@code delete} 的内存缓存。
 * 业务服务若需数据库持久化，只需自行声明一个 {@link WorkflowDefinitionRepository} 类型的
 * Bean 即可覆盖本默认实现（{@link ConditionalOnMissingBean}）。</p>
 *
 * <p>JSON 文件示例结构（字段与 {@link WorkflowDefinition} 一一对应）：
 * <pre>
 * {
 *   "id": "demo",
 *   "name": "示例工作流",
 *   "status": "PUBLISHED",
 *   "nodes": [ ... ],
 *   "edges": [ ... ]
 * }
 * </pre>
 * </p>
 */
@Slf4j
@Component
@ConditionalOnMissingBean(WorkflowDefinitionRepository.class)
public class InMemoryWorkflowDefinitionRepository implements WorkflowDefinitionRepository {

    /** classpath 下工作流定义 JSON 的扫描路径 */
    private static final String DEFINITION_RESOURCE_PATTERN = "classpath*:/workflows/*.json";

    private final Map<String, WorkflowDefinition> store = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceResolver;

    public InMemoryWorkflowDefinitionRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    @PostConstruct
    public void loadDefinitionsFromClasspath() {
        try {
            Resource[] resources = resourceResolver.getResources(DEFINITION_RESOURCE_PATTERN);
            for (Resource resource : resources) {
                try {
                    WorkflowDefinition definition = objectMapper.readValue(
                            resource.getInputStream(), WorkflowDefinition.class);
                    if (definition == null || definition.getId() == null) {
                        log.warn("Skipping invalid workflow definition file: {}", resource.getFilename());
                        continue;
                    }
                    store.put(definition.getId(), definition);
                    log.info("Loaded workflow definition '{}' from {}",
                            definition.getId(), resource.getFilename());
                } catch (IOException e) {
                    log.error("Failed to parse workflow definition file: {}", resource.getFilename(), e);
                }
            }
            log.info("Workflow definition repository initialized with {} definitions", store.size());
        } catch (IOException e) {
            log.error("Failed to scan workflow definition resources", e);
        }
    }

    @Override
    public WorkflowDefinition getById(String definitionId) {
        return store.get(definitionId);
    }

    @Override
    public List<WorkflowDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    @Override
    public void save(WorkflowDefinition definition) {
        if (definition == null || definition.getId() == null) {
            throw new IllegalArgumentException("WorkflowDefinition and its id must not be null");
        }
        store.put(definition.getId(), definition);
        log.debug("Saved workflow definition: {}", definition.getId());
    }

    @Override
    public boolean delete(String definitionId) {
        return store.remove(definitionId) != null;
    }
}
