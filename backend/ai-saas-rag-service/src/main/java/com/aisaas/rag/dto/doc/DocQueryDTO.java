package com.aisaas.rag.dto.doc;

import lombok.Data;

@Data
public class DocQueryDTO {
    private String kbId;
    private String docType;
    private Integer processStatus;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
