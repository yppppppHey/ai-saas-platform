package com.aisaas.rag.dto.kb;

import lombok.Data;

@Data
public class KbQueryDTO {
    private String keyword;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
