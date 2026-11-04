package com.aisaas.rag.dto.kb;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KbAccessLogDTO {
    private Long id;
    private String kbId;
    private Long userId;
    private String action;
    private String ipAddress;
    private LocalDateTime createdAt;
}
