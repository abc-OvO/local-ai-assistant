package com.example.localai.service.impl;

import com.example.localai.client.OllamaClient;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.ChatResponse;
import com.example.localai.dto.OllamaGenerateResponse;
import com.example.localai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final OllamaClient ollamaClient;

    private final OllamaProperties properties;

    @Override
    public ChatResponse chat(String message) {
        OllamaGenerateResponse ollamaResponse = ollamaClient.generate(message);
        String model = ollamaResponse.getModel() == null ? properties.getModel() : ollamaResponse.getModel();
        return new ChatResponse(model, ollamaResponse.getResponse());
    }
}
