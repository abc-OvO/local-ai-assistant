package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {

    private String sessionId;

    private Integer messageCount;

    private LocalDateTime lastMessageAt;
}
