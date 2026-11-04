package com.aisaas.rag.dto.retrieval;

import lombok.Data;

@Data
public class FeedbackDTO {
    private String feedbackType;
    private String comment;
    private Integer rating;
}
