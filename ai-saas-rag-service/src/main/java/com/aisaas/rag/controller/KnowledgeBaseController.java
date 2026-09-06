package com.aisaas.rag.controller;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.kb.*;
import com.aisaas.rag.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService kbService;

    @PostMapping
    public Result<KbResponseDTO> createKb(@Valid @RequestBody KbCreateDTO dto) {
        Long userId = getCurrentUserId();
        log.info("创建知识库: userId={}, kbName={}", userId, dto.getKbName());
        return kbService.createKb(dto, userId);
    }

    @GetMapping("/{kbId}")
    public Result<KbResponseDTO> getKb(@PathVariable String kbId) {
        Long userId = getCurrentUserId();
        log.debug("获取知识库详情: kbId={}, userId={}", kbId, userId);
        return kbService.getKb(kbId, userId);
    }

    @GetMapping
    public Result<?> listKbs(KbQueryDTO queryDTO) {
        Long userId = getCurrentUserId();
        log.debug("查询知识库列表: userId={}, pageNum={}, pageSize={}",
                userId, queryDTO.getPageNum(), queryDTO.getPageSize());
        return kbService.listKbs(queryDTO, userId);
    }

    @PutMapping("/{kbId}")
    public Result<KbResponseDTO> updateKb(@PathVariable String kbId, @RequestBody KbUpdateDTO dto) {
        Long userId = getCurrentUserId();
        log.info("更新知识库: kbId={}, userId={}", kbId, userId);
        return kbService.updateKb(kbId, dto, userId);
    }

    @DeleteMapping("/{kbId}")
    public Result<Boolean> deleteKb(@PathVariable String kbId) {
        Long userId = getCurrentUserId();
        log.info("删除知识库: kbId={}, userId={}", kbId, userId);
        return kbService.deleteKb(kbId, userId);
    }

    @GetMapping("/{kbId}/statistics")
    public Result<KbStatisticsDTO> getStatistics(@PathVariable String kbId) {
        Long userId = getCurrentUserId();
        log.debug("获取知识库统计: kbId={}, userId={}", kbId, userId);
        return kbService.getKbStatistics(kbId, userId);
    }

    @PostMapping("/{kbId}/rebuild")
    public Result<Boolean> rebuildIndex(@PathVariable String kbId) {
        Long userId = getCurrentUserId();
        log.info("重建知识库索引: kbId={}, userId={}", kbId, userId);
        return kbService.rebuildIndex(kbId, userId);
    }

    @GetMapping("/{kbId}/access-logs")
    public Result<List<KbAccessLogDTO>> getAccessLogs(@PathVariable String kbId) {
        Long userId = getCurrentUserId();
        log.debug("获取知识库访问日志: kbId={}, userId={}", kbId, userId);
        return kbService.getAccessLogs(kbId, userId);
    }

    private Long getCurrentUserId() {
        return 1L;
    }
}
