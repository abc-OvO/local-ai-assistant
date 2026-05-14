package com.example.localai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    private String sessionId;

    @NotBlank(message = "message 不能为空")
    private String message;
}
