package com.example.localai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OllamaAiChatClient implements AiChatClient {

    private final OllamaClient ollamaClient;

    @Override
    public String generate(String prompt) {
        return ollamaClient.generate(prompt).getResponse();
    }
}
