package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.util.List;

@Data
public class RelevanceRankingDTO {
    private String query;
    private String method;
    private List<RetrievalResultDTO> results;
}
