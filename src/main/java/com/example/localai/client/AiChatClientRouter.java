package com.example.localai.client;

import com.example.localai.config.AiProperties;
import com.example.localai.config.KimiProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        String provider = normalizeProvider();
        long start = System.currentTimeMillis();
        System.out.println("[AiChatClientRouter] generate start, provider=" + provider
                + ", promptLength=" + prompt.length());
        try {
            String reply = currentClient(provider).generate(prompt);
            long cost = System.currentTimeMillis() - start;
            System.out.println("[AiChatClientRouter] generate success, provider=" + provider
                    + ", promptLength=" + prompt.length()
                    + ", costMs=" + cost);
            return reply;
        } catch (RuntimeException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[AiChatClientRouter] generate failed, provider=" + provider
                    + ", promptLength=" + prompt.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw ex;
        }
    }

    public String currentProvider() {
        return normalizeProvider();
    }

    public String switchProvider(String provider) {
        aiProperties.switchProvider(provider);
        return currentProvider();
    }

    public String currentModel() {
        return switch (normalizeProvider()) {
            case "ollama" -> ollamaProperties.getModel();
            case "kimi" -> kimiProperties.getModel();
            default -> throw new BusinessException(500, "不支持的 AI provider：" + aiProperties.getProvider());
        };
    }

    private AiChatClient currentClient() {
        return currentClient(normalizeProvider());
    }

    private AiChatClient currentClient(String provider) {
        return switch (provider) {
            case "ollama" -> ollamaAiChatClient;
            case "kimi" -> kimiAiChatClient;
            default -> throw new BusinessException(500, "不支持的 AI provider：" + aiProperties.getProvider());
        };
    }

    private String normalizeProvider() {
        return aiProperties.normalizedProvider();
    }
}
