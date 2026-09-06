package com.aisaas.rag.service;

import com.aisaas.common.result.Result;
import com.aisaas.rag.dto.kb.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public interface KnowledgeBaseService {
    Result<KbResponseDTO> createKb(KbCreateDTO dto, Long userId);
    Result<KbResponseDTO> getKb(String kbId, Long userId);
    Result<IPage<KbResponseDTO>> listKbs(KbQueryDTO queryDTO, Long userId);
    Result<KbResponseDTO> updateKb(String kbId, KbUpdateDTO dto, Long userId);
    Result<Boolean> deleteKb(String kbId, Long userId);
    Result<KbStatisticsDTO> getKbStatistics(String kbId, Long userId);
    Result<Boolean> rebuildIndex(String kbId, Long userId);
    Result<List<KbAccessLogDTO>> getAccessLogs(String kbId, Long userId);
}
