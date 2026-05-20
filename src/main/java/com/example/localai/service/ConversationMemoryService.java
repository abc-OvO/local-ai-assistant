package com.example.localai.service;

import com.example.localai.model.ChatMessage;
import com.example.localai.dto.ChatSessionResponse;

import java.util.List;

public interface ConversationMemoryService {

    String normalizeSessionId(String sessionId);

    List<ChatMessage> getRecentMessages(String sessionId);

    void appendSuccessfulTurn(String sessionId, String userMessage, String assistantMessage);

    void clear(String sessionId);

    boolean isEnabled();

    int historyTurns(String sessionId);

    String formatHistory(List<ChatMessage> messages);

    List<ChatSessionResponse> listSessions();
}
