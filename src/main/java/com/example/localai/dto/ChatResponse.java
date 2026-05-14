package com.example.localai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatResponse {

    private String model;

    private String reply;

    private String sessionId;

    private Boolean memoryEnabled;

    private Integer historyTurns;

    public ChatResponse(String model, String reply) {
        this.model = model;
        this.reply = reply;
    }

    public ChatResponse(String model, String reply, String sessionId, Boolean memoryEnabled, Integer historyTurns) {
        this.model = model;
        this.reply = reply;
        this.sessionId = sessionId;
        this.memoryEnabled = memoryEnabled;
        this.historyTurns = historyTurns;
    }
}
