package com.example.localai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GlobalKnowledgeAskRequest {

    @NotBlank(message = "question 不能为空")
    private String question;
}
