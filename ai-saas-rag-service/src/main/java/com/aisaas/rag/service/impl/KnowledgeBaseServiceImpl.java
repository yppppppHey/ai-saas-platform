package com.aisaas.rag.service.impl;

import com.aisaas.common.result.Result;
import com.aisaas.common.result.ResultCode;
import com.aisaas.rag.constant.KbAccessLevelEnum;
import com.aisaas.rag.constant.KbStatusEnum;
import com.aisaas.rag.dto.kb.*;
import com.aisaas.rag.entity.RagKnowledgeBase;
import com.aisaas.rag.mapper.RagKnowledgeBaseMapper;
import com.aisaas.rag.service.KnowledgeBaseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private RagKnowledgeBaseMapper kbMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<KbResponseDTO> createKb(KbCreateDTO dto, Long userId) {
        String kbId = "kb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        RagKnowledgeBase kb = new RagKnowledgeBase();
        kb.setKbId(kbId);
        kb.setKbName(dto.getKbName());
        kb.setDescription(dto.getDescription());
        kb.setUserId(userId);
        kb.setEmbeddingModel(dto.getEmbeddingModel() != null ? dto.getEmbeddingModel() : "text-embedding-3-small");
        kb.setVectorStore(dto.getVectorStore() != null ? dto.getVectorStore() : "qdrant");
        kb.setDimension(dto.getDimension() != null ? dto.getDimension() : 1536);
        kb.setChunkSize(dto.getChunkSize() != null ? dto.getChunkSize() : 1000);
        kb.setChunkOverlap(dto.getChunkOverlap() != null ? dto.getChunkOverlap() : 200);
        kb.setTotalDocuments(0);
        kb.setTotalChunks(0);
        kb.setTotalSize(0L);
        kb.setAccessLevel(dto.getAccessLevel() != null ? dto.getAccessLevel() : KbAccessLevelEnum.PRIVATE.getCode());
        kb.setTeamId(dto.getTeamId());
        kb.setStatus(KbStatusEnum.ACTIVE.getCode());
        kb.setBuildProgress(0);
        kb.setIsDeleted(0);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());

        kbMapper.insert(kb);
        log.info("创建知识库成功: kbId={}, userId={}, kbName={}", kbId, userId, dto.getKbName());

        return Result.success(convertToDTO(kb));
    }

    @Override
    public Result<KbResponseDTO> getKb(String kbId, Long userId) {
        RagKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId) && !hasAccess(kb, userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权访问该知识库");
        }
        return Result.success(convertToDTO(kb));
    }

    @Override
    public Result<IPage<KbResponseDTO>> listKbs(KbQueryDTO queryDTO, Long userId) {
        Page<RagKnowledgeBase> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<RagKnowledgeBase> kbPage = kbMapper.selectKbPage(page, userId, queryDTO.getKeyword(), queryDTO.getStatus());

        List<KbResponseDTO> dtoList = kbPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<KbResponseDTO> resultPage = new Page<>(kbPage.getCurrent(), kbPage.getSize(), kbPage.getTotal());
        resultPage.setRecords(dtoList);
        return Result.success(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<KbResponseDTO> updateKb(String kbId, KbUpdateDTO dto, Long userId) {
        RagKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权修改该知识库");
        }

        if (dto.getKbName() != null) kb.setKbName(dto.getKbName());
        if (dto.getDescription() != null) kb.setDescription(dto.getDescription());
        if (dto.getAccessLevel() != null) kb.setAccessLevel(dto.getAccessLevel());
        if (dto.getChunkSize() != null) kb.setChunkSize(dto.getChunkSize());
        if (dto.getChunkOverlap() != null) kb.setChunkOverlap(dto.getChunkOverlap());

        kb.setUpdatedAt(LocalDateTime.now());
        kbMapper.updateById(kb);

        log.info("更新知识库成功: kbId={}, userId={}", kbId, userId);
        return Result.success(convertToDTO(kb));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteKb(String kbId, Long userId) {
        RagKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权删除该知识库");
        }

        kb.setIsDeleted(1);
        kb.setUpdatedAt(LocalDateTime.now());
        kbMapper.updateById(kb);

        log.info("删除知识库成功: kbId={}, userId={}", kbId, userId);
        return Result.success(true);
    }

    @Override
    public Result<KbStatisticsDTO> getKbStatistics(String kbId, Long userId) {
        RagKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "知识库不存在");
        }

        KbStatisticsDTO stats = new KbStatisticsDTO();
        stats.setKbId(kbId);
        stats.setTotalDocuments(kb.getTotalDocuments());
        stats.setTotalChunks(kb.getTotalChunks());
        stats.setTotalSize(kb.getTotalSize());
        stats.setStatus(kb.getStatus());
        stats.setStatusDesc(KbStatusEnum.fromCode(kb.getStatus()).getName());
        stats.setLastBuildAt(kb.getLastBuildAt());

        return Result.success(stats);
    }

    @Override
    public Result<Boolean> rebuildIndex(String kbId, Long userId) {
        RagKnowledgeBase kb = kbMapper.selectByKbId(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            return Result.error(ResultCode.NOT_FOUND, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            return Result.error(ResultCode.FORBIDDEN, "无权操作该知识库");
        }
        if (!KbStatusEnum.canBuildIndex(kb.getStatus())) {
            return Result.error(ResultCode.BIZ_ERROR, "当前状态不允许重建索引");
        }

        kb.setStatus(KbStatusEnum.BUILDING.getCode());
        kb.setBuildProgress(0);
        kb.setUpdatedAt(LocalDateTime.now());
        kbMapper.updateById(kb);

        log.info("开始重建知识库索引: kbId={}, userId={}", kbId, userId);
        return Result.success(true);
    }

    @Override
    public Result<List<KbAccessLogDTO>> getAccessLogs(String kbId, Long userId) {
        return Result.success(List.of());
    }

    private boolean hasAccess(RagKnowledgeBase kb, Long userId) {
        if (kb.getAccessLevel() == KbAccessLevelEnum.PUBLIC.getCode()) {
            return true;
        }
        if (kb.getAccessLevel() == KbAccessLevelEnum.TEAM.getCode() && kb.getTeamId() != null) {
            return true;
        }
        return false;
    }

    private KbResponseDTO convertToDTO(RagKnowledgeBase kb) {
        KbResponseDTO dto = new KbResponseDTO();
        BeanUtils.copyProperties(kb, dto);
        dto.setKbId(kb.getKbId());
        dto.setStatusDesc(KbStatusEnum.fromCode(kb.getStatus()).getName());
        dto.setAccessLevelDesc(KbAccessLevelEnum.fromCode(kb.getAccessLevel()).getName());
        return dto;
    }
}
