package com.example.localai.dto;

import com.example.localai.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryResponse {

    private String sessionId;

    private Boolean memoryEnabled;

    private Integer messageCount;

    private Integer historyTurns;

    private List<ChatMessage> messages;
}
