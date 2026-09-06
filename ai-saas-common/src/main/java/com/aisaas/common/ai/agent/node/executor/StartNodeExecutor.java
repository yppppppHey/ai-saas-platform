package com.aisaas.common.ai.agent.node.executor;

import com.aisaas.common.ai.agent.node.NodeExecutionContext;
import com.aisaas.common.ai.agent.node.NodeExecutionResult;
import com.aisaas.common.ai.agent.node.NodeExecutor;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 开始节点执行器
 * 处理工作流的开始节点
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_START;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing start node: {}", node.getId());

        Map<String, Object> outputs = new HashMap<>();

        // 将输入参数作为输出
        if (context.getInputs() != null) {
            outputs.putAll(context.getInputs());
        }

        // 记录开始信息
        outputs.put("_startTime", java.time.LocalDateTime.now().toString());
        outputs.put("_instanceId", context.getInstanceId());

        log.info("Workflow started: instanceId={}", context.getInstanceId());

        return NodeExecutionResult.success(outputs);
    }
}
