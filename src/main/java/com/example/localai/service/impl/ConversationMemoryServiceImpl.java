package com.example.localai.service.impl;

import com.example.localai.config.AppProperties;
import com.example.localai.dto.ChatSessionResponse;
import com.example.localai.mapper.ChatHistoryMapper;
import com.example.localai.model.ChatMessage;
import com.example.localai.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final ChatHistoryMapper chatHistoryMapper;

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

        String normalizedSessionId = normalizeSessionId(sessionId);
        int maxMessages = maxMessages();
        if (maxMessages <= 0) {
            return List.of();
        }

        List<ChatMessage> persistedMessages = chatHistoryMapper.findRecentMessages(normalizedSessionId, maxMessages);
        refreshCache(normalizedSessionId, persistedMessages);
        return persistedMessages;
    }

    @Override
    @Transactional
    public void appendSuccessfulTurn(String sessionId, String userMessage, String assistantMessage) {
        if (!isEnabled()) {
            return;
        }

        String normalizedSessionId = normalizeSessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        ChatMessage user = new ChatMessage("user", userMessage, now);
        ChatMessage assistant = new ChatMessage("assistant", assistantMessage, now);

        chatHistoryMapper.insert(normalizedSessionId, user);
        chatHistoryMapper.insert(normalizedSessionId, assistant);
        appendCache(normalizedSessionId, user, assistant);
    }

    @Override
    @Transactional
    public void clear(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        chatHistoryMapper.deleteBySessionId(normalizedSessionId);
        memory.remove(normalizedSessionId);
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

    @Override
    public List<ChatSessionResponse> listSessions() {
        if (!isEnabled()) {
            return List.of();
        }
        return chatHistoryMapper.findSessions();
    }

    private void trim(Deque<ChatMessage> messages) {
        int maxMessages = maxMessages();
        while (messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }

    private int maxMessages() {
        return Math.max(appProperties.getMemory().getMaxTurns(), 0) * 2;
    }

    private void appendCache(String sessionId, ChatMessage user, ChatMessage assistant) {
        Deque<ChatMessage> messages = memory.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (messages) {
            messages.addLast(user);
            messages.addLast(assistant);
            trim(messages);
        }
    }

    private void refreshCache(String sessionId, List<ChatMessage> persistedMessages) {
        Deque<ChatMessage> messages = memory.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (messages) {
            messages.clear();
            messages.addAll(new ArrayList<>(persistedMessages));
            trim(messages);
        }
    }
}
