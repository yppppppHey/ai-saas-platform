package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.Map;

@Data
public class HybridSearchDTO {
    private String kbId;
    private String query;
    private Integer topK;
    private Double minScore;
    private Double vectorWeight;
    private Double keywordWeight;
    private Map<String, Object> metadataFilter;
}
