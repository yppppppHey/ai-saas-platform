package com.aisaas.rag.dto.kb;

import lombok.Data;

@Data
public class KbUpdateDTO {
    private String kbName;
    private String description;
    private Integer accessLevel;
    private Integer chunkSize;
    private Integer chunkOverlap;
}
