package com.example.localai.client;

import com.example.localai.config.AiProperties;
import com.example.localai.config.KimiProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiChatClientRouter implements AiChatClient {

    private final AiProperties aiProperties;

    private final OllamaAiChatClient ollamaAiChatClient;

    private final KimiAiChatClient kimiAiChatClient;

    private final OllamaProperties ollamaProperties;

    private final KimiProperties kimiProperties;

    @Override
    public String generate(String prompt) {
        return currentClient().generate(prompt);
    }

    public String currentProvider() {
        return normalizeProvider();
    }

    public String currentModel() {
        return switch (normalizeProvider()) {
            case "ollama" -> ollamaProperties.getModel();
            case "kimi" -> kimiProperties.getModel();
            default -> throw new BusinessException(500, "不支持的 AI provider：" + aiProperties.getProvider());
        };
    }

    private AiChatClient currentClient() {
        return switch (normalizeProvider()) {
            case "ollama" -> ollamaAiChatClient;
            case "kimi" -> kimiAiChatClient;
            default -> throw new BusinessException(500, "不支持的 AI provider：" + aiProperties.getProvider());
        };
    }

    private String normalizeProvider() {
        return aiProperties.getProvider() == null
                ? "ollama"
                : aiProperties.getProvider().trim().toLowerCase(Locale.ROOT);
    }
}
