package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.List;

@Data
public class RagAnswerDTO {
    private String answer;
    private List<RetrievalResultDTO> sources;
    private Integer latencyMs;
    private Integer tokenUsage;
    private String model;
}
