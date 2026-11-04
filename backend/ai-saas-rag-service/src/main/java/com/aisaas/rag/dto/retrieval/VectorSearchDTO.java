package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.Map;

@Data
public class VectorSearchDTO {
    private String kbId;
    private String query;
    private Integer topK;
    private Double minScore;
    private Map<String, Object> metadataFilter;
}
