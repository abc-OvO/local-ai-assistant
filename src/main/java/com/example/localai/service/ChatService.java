package com.example.localai.service;

import com.example.localai.dto.ChatResponse;

public interface ChatService {

    ChatResponse chat(String message);

    ChatResponse chat(String sessionId, String message);
}
