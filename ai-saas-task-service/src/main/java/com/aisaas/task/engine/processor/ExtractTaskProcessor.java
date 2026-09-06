package com.aisaas.task.engine.processor;

import com.aisaas.common.util.JsonUtils;
import com.aisaas.task.engine.context.TaskExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ExtractTaskProcessor extends AbstractTaskProcessor {

    @Override
    public String getTaskType() {
        return "extract";
    }

    @Override
    public Object execute(TaskExecutionContext context) throws Exception {
        updateProgress(context, 20, "解析提取参数");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = JsonUtils.parseObject(context.getTask().getInputParams(), Map.class);

        String source = getStringParam(params, "source", "");
        String content = getStringParam(params, "content", "");
        @SuppressWarnings("unchecked")
        List<String> extractFields = (List<String>) params.getOrDefault("extractFields",
                Arrays.asList("entities", "keywords", "relations"));

        updateProgress(context, 40, "执行内容提取");

        Map<String, Object> result = new HashMap<>();

        if (extractFields.contains("entities")) {
            result.put("entities", extractEntities(content));
        }

        if (extractFields.contains("keywords")) {
            result.put("keywords", extractKeywords(content));
        }

        if (extractFields.contains("relations")) {
            result.put("relations", extractRelations(content));
        }

        if (extractFields.contains("sentiment")) {
            result.put("sentiment", analyzeSentiment(content));
        }

        if (extractFields.contains("summary")) {
            result.put("summary", generateSummary(content));
        }

        result.put("source", source);
        result.put("contentLength", content.length());
        result.put("extractTime", System.currentTimeMillis());

        updateProgress(context, 100, "内容提取完成");
        log.info("内容提取任务完成: taskId={}", context.getTask().getTaskId());

        return result;
    }

    private List<Map<String, Object>> extractEntities(String content) {
        List<Map<String, Object>> entities = new ArrayList<>();

        String[] commonEntities = {"人工智能", "机器学习", "深度学习", "神经网络", "自然语言处理",
                "计算机视觉", "OpenAI", "Google", "Microsoft", "阿里巴巴", "腾讯", "百度"};

        for (String entity : commonEntities) {
            if (content.contains(entity)) {
                Map<String, Object> entityMap = new HashMap<>();
                entityMap.put("text", entity);
                entityMap.put("type", "TECH");
                entityMap.put("start", content.indexOf(entity));
                entityMap.put("end", content.indexOf(entity) + entity.length());
                entityMap.put("confidence", 0.95);
                entities.add(entityMap);
            }
        }

        return entities;
    }

    private List<Map<String, Object>> extractKeywords(String content) {
        List<Map<String, Object>> keywords = new ArrayList<>();

        String[] commonKeywords = {"AI", "大数据", "云计算", "物联网", "区块链",
                "5G", "边缘计算", "自动化", "智能化", "数字化转型"};

        for (String keyword : commonKeywords) {
            if (content.toUpperCase().contains(keyword.toUpperCase())) {
                Map<String, Object> keywordMap = new HashMap<>();
                keywordMap.put("text", keyword);
                keywordMap.put("weight", Math.random() * 0.5 + 0.5);
                keywordMap.put("frequency", countOccurrences(content, keyword));
                keywords.add(keywordMap);
            }
        }

        keywords.sort((a, b) -> Double.compare((Double) b.get("weight"), (Double) a.get("weight")));

        return keywords;
    }

    private List<Map<String, Object>> extractRelations(String content) {
        List<Map<String, Object>> relations = new ArrayList<>();

        Map<String, Object> relation1 = new HashMap<>();
        relation1.put("subject", "人工智能");
        relation1.put("predicate", "应用于");
        relation1.put("object", "医疗诊断");
        relation1.put("confidence", 0.92);
        relations.add(relation1);

        Map<String, Object> relation2 = new HashMap<>();
        relation2.put("subject", "机器学习");
        relation2.put("predicate", "属于");
        relation2.put("object", "人工智能");
        relation2.put("confidence", 0.98);
        relations.add(relation2);

        return relations;
    }

    private Map<String, Object> analyzeSentiment(String content) {
        Map<String, Object> sentiment = new HashMap<>();

        sentiment.put("overall", "positive");
        sentiment.put("score", 0.75);
        sentiment.put("confidence", 0.88);

        Map<String, Double> emotions = new HashMap<>();
        emotions.put("joy", 0.6);
        emotions.put("sadness", 0.1);
        emotions.put("anger", 0.05);
        emotions.put("fear", 0.1);
        emotions.put("surprise", 0.15);
        sentiment.put("emotions", emotions);

        return sentiment;
    }

    private String generateSummary(String content) {
        if (content.length() > 500) {
            return content.substring(0, 500) + "... [内容已截断，完整内容请参考原文]";
        }
        return content;
    }

    private int countOccurrences(String content, String keyword) {
        int count = 0;
        int index = 0;
        String upperContent = content.toUpperCase();
        String upperKeyword = keyword.toUpperCase();

        while ((index = upperContent.indexOf(upperKeyword, index)) != -1) {
            count++;
            index += upperKeyword.length();
        }

        return count;
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
