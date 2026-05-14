package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.dto.ChatResponse;
import com.example.localai.service.ChatService;
import com.example.localai.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatClientRouter aiChatClientRouter;

    private final ConversationMemoryService conversationMemoryService;

    @Override
    public ChatResponse chat(String message) {
        return chat(null, message);
    }

    @Override
    public ChatResponse chat(String sessionId, String message) {
        String normalizedSessionId = conversationMemoryService.normalizeSessionId(sessionId);
        String history = conversationMemoryService.formatHistory(
                conversationMemoryService.getRecentMessages(normalizedSessionId)
        );
        String prompt = buildPrompt(history, message);

        String reply = aiChatClientRouter.generate(prompt);
        conversationMemoryService.appendSuccessfulTurn(normalizedSessionId, message, reply);
        int historyTurns = conversationMemoryService.historyTurns(normalizedSessionId);

        System.out.println("[ChatMemory] endpoint=/api/chat"
                + ", sessionId=" + normalizedSessionId
                + ", memoryEnabled=" + conversationMemoryService.isEnabled()
                + ", historyTurns=" + historyTurns
                + ", promptLength=" + prompt.length()
                + ", generationProvider=" + aiChatClientRouter.currentProvider());

        return new ChatResponse(
                aiChatClientRouter.currentModel(),
                reply,
                normalizedSessionId,
                conversationMemoryService.isEnabled(),
                historyTurns
        );
    }

    private String buildPrompt(String history, String message) {
        return """
            你是一个简洁、准确的中文 AI 助手。
            以下是最近的对话历史：
            %s

            用户当前问题：
            %s

            请结合上下文回答当前问题。
            """.formatted(history, message);
    }
}
