package com.aisaas.rag.dto.doc;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocVersionDTO {
    private String docId;
    private String docName;
    private Integer currentVersion;
    private List<VersionInfo> versions;

    @Data
    public static class VersionInfo {
        private Long versionId;
        private Integer version;
        private Long size;
        private String changeLog;
        private LocalDateTime createdAt;
        private Boolean isLatest;
    }
}
