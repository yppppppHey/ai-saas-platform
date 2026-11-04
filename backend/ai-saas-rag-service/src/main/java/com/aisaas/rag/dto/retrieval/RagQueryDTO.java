package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.Map;

@Data
public class RagQueryDTO {
    private String kbId;
    private String query;
    private String systemPrompt;
    private String modelId;
    private Integer topK;
    private Double minScore;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, Object> metadataFilter;
}
