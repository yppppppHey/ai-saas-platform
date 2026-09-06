package com.aisaas.rag.service;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.retrieval.*;
import java.util.List;

public interface RetrievalService {
    Result<List<RetrievalResultDTO>> vectorSearch(VectorSearchDTO dto, Long userId);
    Result<List<RetrievalResultDTO>> hybridSearch(HybridSearchDTO dto, Long userId);
    Result<RagAnswerDTO> ragQuery(RagQueryDTO dto, Long userId);
    Result<List<String>> getSuggestedQuestions(String kbId, Long userId);
    Result<Boolean> submitFeedback(String docId, FeedbackDTO dto, Long userId);
    Result<SearchHistoryDTO> getSearchHistory(String kbId, Long userId);
    Result<RelevanceRankingDTO> rerankResults(List<RetrievalResultDTO> results, String query);
}
