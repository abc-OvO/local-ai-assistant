package com.example.localai.service.impl;

import com.example.localai.config.AppProperties;
import com.example.localai.model.ChatMessage;
import com.example.localai.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private static final String DEFAULT_SESSION_ID = "default";

    private final AppProperties appProperties;

    private final Map<String, Deque<ChatMessage>> memory = new ConcurrentHashMap<>();

    @Override
    public String normalizeSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return DEFAULT_SESSION_ID;
        }
        return sessionId.trim();
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId) {
        if (!isEnabled()) {
            return List.of();
        }

        Deque<ChatMessage> messages = memory.get(normalizeSessionId(sessionId));
        if (messages == null) {
            return List.of();
        }
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    @Override
    public void appendSuccessfulTurn(String sessionId, String userMessage, String assistantMessage) {
        if (!isEnabled()) {
            return;
        }

        String normalizedSessionId = normalizeSessionId(sessionId);
        Deque<ChatMessage> messages = memory.computeIfAbsent(normalizedSessionId, key -> new ArrayDeque<>());
        synchronized (messages) {
            LocalDateTime now = LocalDateTime.now();
            messages.addLast(new ChatMessage("user", userMessage, now));
            messages.addLast(new ChatMessage("assistant", assistantMessage, now));
            trim(messages);
        }
    }

    @Override
    public void clear(String sessionId) {
        memory.remove(normalizeSessionId(sessionId));
    }

    @Override
    public boolean isEnabled() {
        return appProperties.getMemory().isEnabled();
    }

    @Override
    public int historyTurns(String sessionId) {
        int messageCount = getRecentMessages(sessionId).size();
        return messageCount / 2;
    }

    @Override
    public String formatHistory(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "无";
        }

        StringBuilder builder = new StringBuilder();
        for (ChatMessage message : messages) {
            String roleName = "assistant".equals(message.getRole()) ? "助手" : "用户";
            builder.append(roleName)
                    .append("：")
                    .append(message.getContent())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private void trim(Deque<ChatMessage> messages) {
        int maxMessages = Math.max(appProperties.getMemory().getMaxTurns(), 0) * 2;
        while (messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }
}
