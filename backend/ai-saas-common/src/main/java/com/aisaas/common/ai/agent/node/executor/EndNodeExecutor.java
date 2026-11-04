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
 * 结束节点执行器
 * 处理工作流的结束节点
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_END;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing end node: {}", node.getId());

        Map<String, Object> outputs = new HashMap<>();

        // 处理输出映射
        if (node.getOutputs() != null) {
            for (Map.Entry<String, String> entry : node.getOutputs().entrySet()) {
                String sourceKey = entry.getKey();
                String outputKey = entry.getValue();

                // 从上下文中获取值
                Object value = context.getVariables().get(sourceKey);
                if (value != null) {
                    outputs.put(outputKey, value);
                }
            }
        }

        // 添加结束信息
        outputs.put("_endTime", java.time.LocalDateTime.now().toString());
        outputs.put("_instanceId", context.getInstanceId());

        // 收集所有输出变量
        Map<String, Object> allOutputs = new HashMap<>();
        allOutputs.putAll(context.getVariables());
        allOutputs.putAll(outputs);

        log.info("Workflow ending: instanceId={}, outputs={}", context.getInstanceId(), outputs.keySet());

        return NodeExecutionResult.success(allOutputs);
    }
}
