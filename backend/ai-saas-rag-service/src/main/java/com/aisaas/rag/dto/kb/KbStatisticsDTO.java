package com.aisaas.rag.dto.kb;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbStatisticsDTO {
    private String kbId;
    private Integer totalDocuments;
    private Integer totalChunks;
    private Long totalSize;
    private Integer status;
    private String statusDesc;
    private LocalDateTime lastBuildAt;
}
