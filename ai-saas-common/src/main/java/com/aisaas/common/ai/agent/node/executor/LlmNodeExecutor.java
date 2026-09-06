package com.aisaas.common.ai.agent.node.executor;

import com.aisaas.common.ai.agent.node.NodeExecutionContext;
import com.aisaas.common.ai.agent.node.NodeExecutionResult;
import com.aisaas.common.ai.agent.node.NodeExecutor;
import com.aisaas.common.ai.agent.workflow.WorkflowNode;
import com.aisaas.common.ai.dto.ChatRequest;
import com.aisaas.common.ai.dto.ChatResponse;
import com.aisaas.common.ai.provider.AIProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LLM节点执行器
 * 调用大语言模型进行对话
 */
@Slf4j
@Component
public class LlmNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return WorkflowNode.TYPE_LLM;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        log.debug("Executing LLM node: {}", node.getId());

        try {
            // 获取配置
            String provider = node.getConfig("provider", "openai").toString();
            String model = node.getConfig("model", "gpt-3.5-turbo").toString();
            String systemPrompt = node.getConfig("systemPrompt", "").toString();
            String userPrompt = node.getConfig("prompt", "").toString();
            Double temperature = node.getConfig("temperature", 0.7);
            Integer maxTokens = node.getConfig("maxTokens", null);

            // 解析提示词中的变量
            userPrompt = resolvePromptVariables(userPrompt, context);
            systemPrompt = resolvePromptVariables(systemPrompt, context);

            // 构建消息列表
            List<ChatRequest.Message> messages = new ArrayList<>();

            // 添加系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(ChatRequest.Message.builder()
                        .role("system")
                        .content(systemPrompt)
                        .build());
            }

            // 添加用户消息
            messages.add(ChatRequest.Message.builder()
                    .role("user")
                    .content(userPrompt)
                    .build());

            // 构建请求
            ChatRequest request = ChatRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();

            // 调用AI Provider
            AIProvider aiProvider = context.getProviderFactory().getProvider(provider);
            if (aiProvider == null) {
                return NodeExecutionResult.failure("AI Provider not found: " + provider);
            }

            long startTime = System.currentTimeMillis();
            ChatResponse response = aiProvider.chat(request);
            long endTime = System.currentTimeMillis();

            if (!response.isSuccess()) {
                return NodeExecutionResult.failure(
                        "LLM call failed: " + (response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error"));
            }

            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("content", response.getContent());
            outputs.put("role", response.getRole());
            outputs.put("model", response.getModel());
            outputs.put("provider", provider);
            outputs.put("latencyMs", endTime - startTime);

            if (response.getUsage() != null) {
                outputs.put("promptTokens", response.getUsage().getPromptTokens());
                outputs.put("completionTokens", response.getUsage().getCompletionTokens());
                outputs.put("totalTokens", response.getUsage().getTotalTokens());
            }

            log.debug("LLM node executed successfully: nodeId={}, latency={}ms",
                    node.getId(), endTime - startTime);

            return NodeExecutionResult.success(outputs);

        } catch (Exception e) {
            log.error("LLM node execution failed: nodeId={}", node.getId(), e);
            return NodeExecutionResult.failure(e.getMessage(), getStackTrace(e));
        }
    }

    /**
     * 解析提示词中的变量
     */
    private String resolvePromptVariables(String prompt, NodeExecutionContext context) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }

        String result = prompt;

        // 处理 ${variable} 格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = null;

            // 优先从输入中获取
            if (context.getInputs() != null) {
                value = context.getInputs().get(varName);
            }

            // 然后从变量中获取
            if (value == null && context.getVariables() != null) {
                value = context.getVariables().get(varName);
            }

            // 最后从上下文中获取
            if (value == null && context.getContext() != null) {
                value = context.getContext().get(varName);
            }

            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
