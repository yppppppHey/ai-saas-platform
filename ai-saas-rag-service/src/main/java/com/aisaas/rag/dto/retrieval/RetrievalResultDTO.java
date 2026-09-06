package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.Map;

@Data
public class RetrievalResultDTO {
    private String docId;
    private String content;
    private Double score;
    private Integer chunkIndex;
    private String documentTitle;
    private Map<String, Object> metadata;
}
