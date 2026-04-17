package com.example.localai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeAskRequest {

    @NotBlank(message = "documentId 不能为空")
    private String documentId;

    @NotBlank(message = "question 不能为空")
    private String question;
}
