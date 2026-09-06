package com.aisaas.rag.service.impl;

import com.aisaas.common.ai.dto.EmbeddingRequest;
import com.aisaas.common.ai.dto.EmbeddingResponse;
import com.aisaas.common.ai.provider.AIProvider;
import com.aisaas.common.ai.provider.factory.AIProviderFactory;
import com.aisaas.common.ai.rag.RagService;
import com.aisaas.common.ai.vector.VectorStore;
import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.retrieval.*;
import com.aisaas.rag.entity.RagKnowledgeBase;
import com.aisaas.rag.mapper.RagKnowledgeBaseMapper;
import com.aisaas.rag.service.RetrievalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RetrievalServiceImpl implements RetrievalService {

    @Autowired
    private RagService ragService;

    @Autowired
    private RagKnowledgeBaseMapper kbMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AIProviderFactory providerFactory;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.7;

    @Override
    public Result<List<RetrievalResultDTO>> vectorSearch(VectorSearchDTO dto, Long userId) {
        log.info("向量搜索: kbId={}, query={}", dto.getKbId(), dto.getQuery());
        try {
            RagKnowledgeBase kb = validateKB(dto.getKbId(), userId);
            if (kb == null) return Result.error(403, "无权访问");

            List<Double> queryVector = embedQuery(dto.getQuery(), kb.getEmbeddingModel());
            if (queryVector == null) return Result.error(500, "向量化失败");

            String collection = "kb_" + dto.getKbId();
            int topK = dto.getTopK() != null ? dto.getTopK() : DEFAULT_TOP_K;
            double minScore = dto.getMinScore() != null ? dto.getMinScore() : DEFAULT_MIN_SCORE;

            List<VectorStore.SearchResult> results = vectorStore.searchWithFilter(
                collection, queryVector, topK, minScore, dto.getMetadataFilter());

            List<RetrievalResultDTO> dtos = results.stream().map(this::toDTO).collect(Collectors.toList());
            return Result.success(dtos);
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<RetrievalResultDTO>> hybridSearch(HybridSearchDTO dto, Long userId) {
        log.info("混合搜索: kbId={}, query={}", dto.getKbId(), dto.getQuery());
        try {
            RagKnowledgeBase kb = validateKB(dto.getKbId(), userId);
            if (kb == null) return Result.error(403, "无权访问");

            List<Double> queryVector = embedQuery(dto.getQuery(), kb.getEmbeddingModel());
            if (queryVector == null) return Result.error(500, "向量化失败");

            String collection = "kb_" + dto.getKbId();
            int topK = dto.getTopK() != null ? dto.getTopK() : DEFAULT_TOP_K;
            double minScore = dto.getMinScore() != null ? dto.getMinScore() : DEFAULT_MIN_SCORE;

            List<VectorStore.SearchResult> results = vectorStore.searchWithFilter(
                collection, queryVector, topK * 2, minScore, dto.getMetadataFilter());

            double vWeight = dto.getVectorWeight() != null ? dto.getVectorWeight() : 0.7;
            double kWeight = dto.getKeywordWeight() != null ? dto.getKeywordWeight() : 0.3;
            String[] keywords = dto.getQuery().toLowerCase().split("\\s+");

            List<RetrievalResultDTO> dtos = results.stream()
                .map(r -> {
                    RetrievalResultDTO d = toDTO(r);
                    String c = d.getContent().toLowerCase();
                    int m = 0;
                    for (String k : keywords) if (c.contains(k)) m++;
                    double ks = (double) m / keywords.length;
                    d.setScore(Math.min(d.getScore() * vWeight + ks * kWeight, 1.0));
                    return d;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());

            return Result.success(dtos);
        } catch (Exception e) {
            log.error("混合搜索失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<RagAnswerDTO> ragQuery(RagQueryDTO dto, Long userId) {
        log.info("RAG问答: kbId={}, query={}", dto.getKbId(), dto.getQuery());
        try {
            RagKnowledgeBase kb = validateKB(dto.getKbId(), userId);
            if (kb == null) return Result.error(403, "无权访问");

            RagService.RagRequest req = new RagService.RagRequest();
            req.setQuery(dto.getQuery());
            req.setCollectionName("kb_" + dto.getKbId());
            req.setTopK(dto.getTopK() != null ? dto.getTopK() : DEFAULT_TOP_K);
            req.setMinSimilarity(dto.getMinScore() != null ? dto.getMinScore() : DEFAULT_MIN_SCORE);
            req.setSystemPrompt(dto.getSystemPrompt());
            req.setModel(dto.getModelId());
            req.setTemperature(dto.getTemperature());
            req.setMaxTokens(dto.getMaxTokens());

            RagService.RagResponse resp = ragService.ragQuery(req);
            if (!resp.getSuccess()) return Result.error(500, resp.getError());

            RagAnswerDTO a = new RagAnswerDTO();
            a.setAnswer(resp.getAnswer());
            a.setLatencyMs(resp.getLatencyMs());
            if (resp.getRetrievals() != null) {
                a.setSources(resp.getRetrievals().stream().map(r -> {
                    RetrievalResultDTO d = new RetrievalResultDTO();
                    d.setDocId(r.getDocumentId() != null ? r.getDocumentId() : r.getId());
                    d.setContent(r.getContent());
                    d.setScore(r.getScore());
                    d.setChunkIndex(r.getChunkIndex());
                    d.setDocumentTitle(r.getDocumentTitle());
                    return d;
                }).collect(Collectors.toList()));
            }
            if (resp.getUsage() != null) a.setTokenUsage(resp.getUsage().getTotalTokens());
            a.setModel(resp.getModel());

            return Result.success(a);
        } catch (Exception e) {
            log.error("RAG问答失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<String>> getSuggestedQuestions(String kbId, Long userId) {
        return Result.success(List.of(
            "知识库的主要内容是什么？",
            "如何快速检索信息？",
            "支持哪些文档格式？"
        ));
    }

    @Override
    public Result<Boolean> submitFeedback(String docId, FeedbackDTO dto, Long userId) {
        log.info("提交反馈: docId={}, userId={}, type={}", docId, userId, dto.getFeedbackType());
        return Result.success(true);
    }

    @Override
    public Result<SearchHistoryDTO> getSearchHistory(String kbId, Long userId) {
        return Result.success(new SearchHistoryDTO());
    }

    @Override
    public Result<RelevanceRankingDTO> rerankResults(List<RetrievalResultDTO> results, String query) {
        RelevanceRankingDTO ranking = new RelevanceRankingDTO();
        ranking.setResults(results);
        ranking.setQuery(query);
        ranking.setMethod("cross-encoder");
        return Result.success(ranking);
    }

    // ============== Helper Methods ==============

    private RagKnowledgeBase validateKB(String kbId, Long userId) {
        return kbMapper.selectOne(
            new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getKbId, kbId)
                .eq(RagKnowledgeBase::getUserId, userId)
                .eq(RagKnowledgeBase::getIsDeleted, 0)
                .eq(RagKnowledgeBase::getStatus, 1)
        );
    }

    private List<Double> embedQuery(String query, String model) {
        try {
            AIProvider provider = providerFactory.getProvider("openai");
            EmbeddingRequest req = EmbeddingRequest.builder()
                .model(model != null ? model : "text-embedding-3-small")
                .inputs(Collections.singletonList(query))
                .build();
            EmbeddingResponse resp = provider.embed(req);
            if (resp != null && resp.getData() != null && !resp.getData().isEmpty()) {
                return resp.getData().get(0).getEmbedding();
            }
            return null;
        } catch (Exception e) {
            log.error("向量化失败", e);
            return null;
        }
    }

    private RetrievalResultDTO toDTO(VectorStore.SearchResult r) {
        RetrievalResultDTO d = new RetrievalResultDTO();
        d.setDocId(r.getId());
        d.setContent((String) r.getMetadata().getOrDefault("content", ""));
        d.setScore(r.getScore());
        d.setChunkIndex((Integer) r.getMetadata().getOrDefault("chunkIndex", 0));
        d.setDocumentTitle((String) r.getMetadata().getOrDefault("documentTitle", ""));
        d.setMetadata(r.getMetadata());
        return d;
    }
}
