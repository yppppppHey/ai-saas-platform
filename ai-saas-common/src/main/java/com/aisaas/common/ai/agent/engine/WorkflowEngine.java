package com.aisaas.common.ai.agent.engine;

import com.aisaas.common.ai.agent.state.StateMachine;
import com.aisaas.common.ai.agent.workflow.WorkflowDefinition;
import com.aisaas.common.ai.agent.workflow.WorkflowDefinitionRepository;
import com.aisaas.common.ai.agent.workflow.WorkflowEdge;
import com.aisaas.common.ai.agent.workflow.WorkflowInstance;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import com.aisaas.common.ai.agent.node.*;
import com.aisaas.common.ai.agent.tool.ToolRegistry;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.AIProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 工作流执行引擎
 * 负责执行工作流实例，管理节点执行和状态转换
 */
@Slf4j
@Component
public class WorkflowEngine {

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private NodeExecutorFactory nodeExecutorFactory;

    // 执行中的实例
    private final Map<String, WorkflowInstance> runningInstances = new ConcurrentHashMap<>();

    // 工作流定义仓储（可选：容器未提供时引擎仅能执行显式传入 definition 的实例）
    @Autowired(required = false)
    private WorkflowDefinitionRepository definitionRepository;

    // 工作流定义本地缓存，避免每次恢复实例都回查仓储
    private final Map<String, WorkflowDefinition> definitionCache = new ConcurrentHashMap<>();

    // 状态机
    private final StateMachine stateMachine = new StateMachine();

    // 线程池
    @org.springframework.beans.factory.annotation.Qualifier("workflowExecutor")
    @org.springframework.beans.factory.annotation.Autowired
    private ExecutorService executorService;

    // 调度器
    @org.springframework.beans.factory.annotation.Qualifier("workflowScheduler")
    @org.springframework.beans.factory.annotation.Autowired
    private ScheduledExecutorService scheduler;

    public WorkflowEngine() {
        initializeStateMachine();
    }

    /**
     * 初始化状态机监听器
     */
    private void initializeStateMachine() {
        // 注册状态监听器
        stateMachine.registerListener(WorkflowInstance.STATUS_RUNNING, new StateMachine.StateListenerAdapter() {
            @Override
            public void onStateEnter(String state, String previousState, String event) {
                log.debug("Workflow instance started running");
            }
        });

        stateMachine.registerListener(WorkflowInstance.STATUS_COMPLETED, new StateMachine.StateListenerAdapter() {
            @Override
            public void onStateEnter(String state, String previousState, String event) {
                log.debug("Workflow instance completed");
            }
        });

        stateMachine.registerListener(WorkflowInstance.STATUS_FAILED, new StateMachine.StateListenerAdapter() {
            @Override
            public void onStateEnter(String state, String previousState, String event) {
                log.debug("Workflow instance failed");
            }
        });
    }

    /**
     * 启动工作流实例
     */
    public WorkflowInstance startWorkflow(WorkflowDefinition definition, Map<String, Object> inputs) {
        return startWorkflow(definition, inputs, null);
    }

    /**
     * 启动工作流实例（带父实例ID）
     */
    public WorkflowInstance startWorkflow(WorkflowDefinition definition, Map<String, Object> inputs, String parentInstanceId) {
        // 验证工作流定义
        WorkflowDefinition.ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Workflow validation failed: " + validation.getErrors());
        }

        // 创建实例
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(UUID.randomUUID().toString())
                .workflowDefinitionId(definition.getId())
                .workflowVersion(definition.getVersion())
                .status(WorkflowInstance.STATUS_PENDING)
                .inputs(inputs != null ? new HashMap<>(inputs) : new HashMap<>())
                .variables(new HashMap<>())
                .outputs(new HashMap<>())
                .executedNodes(new HashMap<>())
                .context(new HashMap<>())
                .parentInstanceId(parentInstanceId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 初始化变量
        initializeVariables(instance, definition, inputs);

        // 存储实例
        runningInstances.put(instance.getId(), instance);

        // 启动执行
        executorService.submit(() -> executeWorkflow(instance, definition));

        log.info("Workflow instance started: instanceId={}, definitionId={}",
                instance.getId(), definition.getId());

        return instance;
    }

    /**
     * 初始化变量
     */
    private void initializeVariables(WorkflowInstance instance, WorkflowDefinition definition, Map<String, Object> inputs) {
        // 设置输入参数
        if (inputs != null) {
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                instance.setVariable(entry.getKey(), entry.getValue());
            }
        }

        // 设置默认值
        if (definition.getVariables() != null) {
            for (Map.Entry<String, WorkflowDefinition.VariableDef> entry : definition.getVariables().entrySet()) {
                String varName = entry.getKey();
                WorkflowDefinition.VariableDef varDef = entry.getValue();

                // 如果变量不存在，则设置默认值
                if (instance.getVariable(varName) == null && varDef.getDefaultValue() != null) {
                    instance.setVariable(varName, varDef.getDefaultValue());
                }
            }
        }
    }

    /**
     * 执行工作流
     */
    private void executeWorkflow(WorkflowInstance instance, WorkflowDefinition definition) {
        try {
            // 更新状态为运行中
            updateInstanceStatus(instance, WorkflowInstance.STATUS_RUNNING, "START");

            // 查找起始节点
            WorkflowNode currentNode = definition.findStartNode();
            if (currentNode == null) {
                throw new IllegalStateException("No start node found in workflow");
            }

            instance.setCurrentNodeId(currentNode.getId());

            // 执行工作流循环
            while (currentNode != null && instance.isActive()) {
                // 执行当前节点
                NodeExecutionResult result = executeNode(instance, definition, currentNode);

                // 处理执行结果
                if (!result.isSuccess()) {
                    // 节点执行失败
                    handleNodeFailure(instance, definition, currentNode, result);
                    break;
                }

                // 记录节点执行
                WorkflowInstance.NodeExecutionRecord record = WorkflowInstance.NodeExecutionRecord.builder()
                        .nodeId(currentNode.getId())
                        .nodeName(currentNode.getName())
                        .nodeType(currentNode.getType())
                        .status("COMPLETED")
                        .outputs(result.getOutputs())
                        .build();
                record.markAsCompleted(result.getOutputs());
                instance.recordNodeExecution(currentNode.getId(), record);

                // 确定下一个节点
                WorkflowNode nextNode = determineNextNode(instance, definition, currentNode, result);

                if (nextNode == null) {
                    // 工作流结束
                    instance.markAsCompleted(instance.getOutputs());
                    log.info("Workflow instance completed: instanceId={}", instance.getId());
                    break;
                }

                currentNode = nextNode;
                instance.setCurrentNodeId(currentNode.getId());
            }

        } catch (Exception e) {
            log.error("Workflow execution failed: instanceId={}", instance.getId(), e);
            instance.markAsFailed(e.getMessage(), getStackTrace(e));
        } finally {
            runningInstances.remove(instance.getId());
        }
    }

    /**
     * 执行节点
     */
    private NodeExecutionResult executeNode(WorkflowInstance instance, WorkflowDefinition definition,
                                           WorkflowNode node) {
        log.debug("Executing node: instanceId={}, nodeId={}, nodeType={}",
                instance.getId(), node.getId(), node.getType());

        try {
            // 准备节点输入
            Map<String, Object> inputs = prepareNodeInputs(instance, definition, node);

            // 创建执行上下文
            NodeExecutionContext context = NodeExecutionContext.builder()
                    .instanceId(instance.getId())
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .inputs(inputs)
                    .variables(instance.getVariables())
                    .context(instance.getContext())
                    .providerFactory(providerFactory)
                    .toolRegistry(toolRegistry)
                    .build();

            // 获取节点执行器并执行
            NodeExecutor executor = nodeExecutorFactory.getExecutor(node.getType());
            if (executor == null) {
                throw new IllegalStateException("No executor found for node type: " + node.getType());
            }

            NodeExecutionResult result = executor.execute(node, context);

            // 保存节点输出到实例变量
            if (result.isSuccess() && result.getOutputs() != null) {
                for (Map.Entry<String, Object> entry : result.getOutputs().entrySet()) {
                    String varName = node.getId() + "." + entry.getKey();
                    instance.setVariable(varName, entry.getValue());

                    // 同时根据outputs映射保存
                    if (node.getOutputs() != null && node.getOutputs().containsKey(entry.getKey())) {
                        String outputAlias = node.getOutputs().get(entry.getKey());
                        instance.setVariable(outputAlias, entry.getValue());
                    }
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Node execution failed: instanceId={}, nodeId={}", instance.getId(), node.getId(), e);
            return NodeExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .errorStack(getStackTrace(e))
                    .build();
        }
    }

    /**
     * 准备节点输入
     */
    private Map<String, Object> prepareNodeInputs(WorkflowInstance instance, WorkflowDefinition definition,
                                                   WorkflowNode node) {
        Map<String, Object> inputs = new HashMap<>();

        // 添加节点定义的输入映射
        if (node.getInputs() != null) {
            for (Map.Entry<String, String> entry : node.getInputs().entrySet()) {
                String paramName = entry.getKey();
                String sourceExpr = entry.getValue();

                // 解析变量表达式（如 ${variableName} 或 ${nodeId.output}）
                Object value = resolveVariableExpression(instance, sourceExpr);
                inputs.put(paramName, value);
            }
        }

        // 添加全局变量作为输入
        for (Map.Entry<String, Object> entry : instance.getVariables().entrySet()) {
            if (!inputs.containsKey(entry.getKey())) {
                inputs.put(entry.getKey(), entry.getValue());
            }
        }

        return inputs;
    }

    /**
     * 解析变量表达式
     */
    private Object resolveVariableExpression(WorkflowInstance instance, String expr) {
        if (expr == null || expr.isEmpty()) {
            return null;
        }

        // 处理 ${...} 格式
        if (expr.startsWith("${") && expr.endsWith("}")) {
            String varPath = expr.substring(2, expr.length() - 1).trim();

            // 处理点号分隔的路径（如 nodeId.outputName）
            if (varPath.contains(".")) {
                String[] parts = varPath.split("\\.", 2);
                String nodeId = parts[0];
                String outputName = parts[1];

                // 获取节点输出
                WorkflowInstance.NodeExecutionRecord record = instance.getNodeExecution(nodeId);
                if (record != null && record.getOutputs() != null) {
                    return record.getOutputs().get(outputName);
                }
            } else {
                // 直接获取变量
                return instance.getVariable(varPath);
            }
        }

        // 如果不是表达式，直接返回值
        return expr;
    }

    /**
     * 确定下一个节点
     */
    private WorkflowNode determineNextNode(WorkflowInstance instance, WorkflowDefinition definition,
                                          WorkflowNode currentNode, NodeExecutionResult result) {
        // 获取所有出边
        List<WorkflowEdge> outgoingEdges = definition.getOutgoingEdges(currentNode.getId());

        if (outgoingEdges.isEmpty()) {
            // 没有出边，工作流结束
            return null;
        }

        // 根据节点类型处理
        if (WorkflowNode.TYPE_CONDITION.equals(currentNode.getType())) {
            // 条件节点：根据条件选择分支
            return evaluateConditionNode(instance, definition, outgoingEdges, result);
        } else if (WorkflowNode.TYPE_LOOP.equals(currentNode.getType())) {
            // 循环节点：检查是否继续循环
            return evaluateLoopNode(instance, definition, currentNode, outgoingEdges, result);
        } else {
            // 普通节点：选择第一个默认边
            return selectDefaultNextNode(definition, outgoingEdges);
        }
    }

    /**
     * 评估条件节点
     */
    private WorkflowNode evaluateConditionNode(WorkflowInstance instance, WorkflowDefinition definition,
                                               List<WorkflowEdge> edges, NodeExecutionResult result) {
        // 获取条件输出
        Object conditionResult = result.getOutputs().get("condition");
        boolean condition = Boolean.TRUE.equals(conditionResult);

        // 根据条件选择边
        for (WorkflowEdge edge : edges) {
            if (condition && WorkflowEdge.TYPE_TRUE.equals(edge.getType())) {
                return definition.findNodeById(edge.getTargetNodeId());
            } else if (!condition && WorkflowEdge.TYPE_FALSE.equals(edge.getType())) {
                return definition.findNodeById(edge.getTargetNodeId());
            }
        }

        // 如果没有匹配的条件边，选择默认边
        return selectDefaultNextNode(definition, edges);
    }

    /**
     * 评估循环节点
     */
    private WorkflowNode evaluateLoopNode(WorkflowInstance instance, WorkflowDefinition definition,
                                          WorkflowNode loopNode, List<WorkflowEdge> edges, NodeExecutionResult result) {
        // 获取循环条件
        Object continueLoop = result.getOutputs().get("continue");

        // 查找循环边和退出边
        WorkflowEdge loopEdge = null;
        WorkflowEdge exitEdge = null;

        for (WorkflowEdge edge : edges) {
            if (WorkflowEdge.TYPE_LOOP.equals(edge.getType())) {
                loopEdge = edge;
            } else {
                exitEdge = edge;
            }
        }

        // 决定是否继续循环
        if (Boolean.TRUE.equals(continueLoop) && loopEdge != null) {
            return definition.findNodeById(loopEdge.getTargetNodeId());
        } else if (exitEdge != null) {
            return definition.findNodeById(exitEdge.getTargetNodeId());
        }

        return null;
    }

    /**
     * 选择默认的下一个节点
     */
    private WorkflowNode selectDefaultNextNode(WorkflowDefinition definition, List<WorkflowEdge> edges) {
        for (WorkflowEdge edge : edges) {
            if (WorkflowEdge.TYPE_DEFAULT.equals(edge.getType())) {
                return definition.findNodeById(edge.getTargetNodeId());
            }
        }
        // 如果没有默认边，选择第一条边
        if (!edges.isEmpty()) {
            return definition.findNodeById(edges.get(0).getTargetNodeId());
        }
        return null;
    }

    /**
     * 处理节点执行失败
     */
    private void handleNodeFailure(WorkflowInstance instance, WorkflowDefinition definition,
                                  WorkflowNode node, NodeExecutionResult result) {
        log.error("Node execution failed: instanceId={}, nodeId={}, error={}",
                instance.getId(), node.getId(), result.getErrorMessage());

        // 记录失败
        WorkflowInstance.NodeExecutionRecord record = WorkflowInstance.NodeExecutionRecord.builder()
                .nodeId(node.getId())
                .nodeName(node.getName())
                .nodeType(node.getType())
                .status("FAILED")
                .build();
        record.markAsFailed(result.getErrorMessage(), result.getErrorStack());
        instance.recordNodeExecution(node.getId(), record);

        // 根据节点的失败处理策略决定下一步
        String onFailure = node.getOnFailure();
        if (WorkflowNode.ON_FAILURE_CONTINUE.equals(onFailure)) {
            // 继续执行，不中断
            log.warn("Continue on failure as configured for node: {}", node.getId());
        } else {
            // 标记实例失败
            instance.markAsFailed(result.getErrorMessage(), result.getErrorStack());
        }
    }

    /**
     * 更新实例状态
     */
    private void updateInstanceStatus(WorkflowInstance instance, String newStatus, String event) {
        String oldStatus = instance.getStatus();
        instance.updateStatus(newStatus);
        log.debug("Instance status changed: {} --[{}]--> {}", oldStatus, event, newStatus);
    }

    /**
     * 暂停工作流实例
     */
    public void pauseInstance(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null && instance.canPause()) {
            updateInstanceStatus(instance, WorkflowInstance.STATUS_PAUSED, "PAUSE");
        }
    }

    /**
     * 恢复工作流实例
     */
    public void resumeInstance(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null && instance.canResume()) {
            WorkflowDefinition definition = getWorkflowDefinition(instance.getWorkflowDefinitionId());
            if (definition != null) {
                updateInstanceStatus(instance, WorkflowInstance.STATUS_RUNNING, "RESUME");
                executorService.submit(() -> continueExecution(instance, definition));
            }
        }
    }

    /**
     * 取消工作流实例
     */
    public void cancelInstance(String instanceId) {
        WorkflowInstance instance = runningInstances.get(instanceId);
        if (instance != null && instance.canCancel()) {
            updateInstanceStatus(instance, WorkflowInstance.STATUS_CANCELLED, "CANCEL");
            runningInstances.remove(instanceId);
        }
    }

    /**
     * 继续执行工作流
     */
    private void continueExecution(WorkflowInstance instance, WorkflowDefinition definition) {
        WorkflowNode currentNode = definition.findNodeById(instance.getCurrentNodeId());
        if (currentNode != null) {
            executeWorkflow(instance, definition);
        }
    }

    /**
     * 获取工作流实例
     */
    public WorkflowInstance getInstance(String instanceId) {
        return runningInstances.get(instanceId);
    }

    /**
     * 获取所有运行中的实例
     */
    public Collection<WorkflowInstance> getRunningInstances() {
        return Collections.unmodifiableCollection(runningInstances.values());
    }

    /**
     * 获取工作流定义（从仓储中加载，并带本地缓存）
     *
     * <p>定义来源由 {@link WorkflowDefinitionRepository} 决定：默认实现会从 classpath
     * 下的 {@code /workflows/*.json} 加载（见 {@code InMemoryWorkflowDefinitionRepository}），
     * 业务服务也可提供基于数据库的实现来覆盖默认 Bean。若容器未提供任何仓储实现，
     * 则此处始终返回 {@code null}，调用方需保证以显式传入 definition 的方式启动实例。</p>
     *
     * @param definitionId 工作流定义ID
     * @return 工作流定义，未找到时返回 {@code null}
     */
    private WorkflowDefinition getWorkflowDefinition(String definitionId) {
        if (definitionId == null) {
            return null;
        }
        WorkflowDefinition cached = definitionCache.get(definitionId);
        if (cached != null) {
            return cached;
        }
        if (definitionRepository == null) {
            log.warn("No WorkflowDefinitionRepository bean found, cannot load definition: {}", definitionId);
            return null;
        }
        WorkflowDefinition definition = definitionRepository.getById(definitionId);
        if (definition == null) {
            log.warn("Workflow definition not found: {}", definitionId);
            return null;
        }
        // 校验定义合法性，非法定义不入缓存
        WorkflowDefinition.ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            log.error("Workflow definition '{}' is invalid: {}", definitionId, validation.getErrors());
            return null;
        }
        definitionCache.put(definitionId, definition);
        return definition;
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
