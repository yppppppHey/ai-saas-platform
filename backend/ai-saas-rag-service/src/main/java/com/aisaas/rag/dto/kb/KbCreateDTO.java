package com.aisaas.rag.dto.kb;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KbCreateDTO {
    @NotBlank(message = "知识库名称不能为空")
    private String kbName;
    private String description;
    private String embeddingModel;
    private String vectorStore;
    private Integer dimension;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer accessLevel;
    private Long teamId;
}
