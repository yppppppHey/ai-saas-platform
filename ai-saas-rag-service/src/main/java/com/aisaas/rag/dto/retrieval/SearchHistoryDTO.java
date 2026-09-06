package com.aisaas.rag.dto.retrieval;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchHistoryDTO {
    private String id;
    private String query;
    private String kbId;
    private LocalDateTime searchTime;
    private Integer resultCount;
}
