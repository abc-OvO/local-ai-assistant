package com.example.localai.controller;

import com.example.localai.common.Result;
import com.example.localai.dto.ChatMemoryResponse;
import com.example.localai.dto.ChatRequest;
import com.example.localai.dto.ChatResponse;
import com.example.localai.dto.ChatSessionResponse;
import com.example.localai.dto.ClearMemoryResponse;
import com.example.localai.service.ChatService;
import com.example.localai.service.ConversationMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private final ConversationMemoryService conversationMemoryService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request.getSessionId(), request.getMessage());
        return Result.success(response);
    }

    @DeleteMapping("/chat/memory/{sessionId}")
    public Result<ClearMemoryResponse> clearMemory(@PathVariable String sessionId) {
        String normalizedSessionId = conversationMemoryService.normalizeSessionId(sessionId);
        conversationMemoryService.clear(normalizedSessionId);
        return Result.success(new ClearMemoryResponse(normalizedSessionId, true));
    }

    @GetMapping("/chat/memory/{sessionId}")
    public Result<ChatMemoryResponse> getMemory(@PathVariable String sessionId) {
        String normalizedSessionId = conversationMemoryService.normalizeSessionId(sessionId);
        var messages = conversationMemoryService.getRecentMessages(normalizedSessionId);
        return Result.success(new ChatMemoryResponse(
                normalizedSessionId,
                conversationMemoryService.isEnabled(),
                messages.size(),
                conversationMemoryService.historyTurns(normalizedSessionId),
                messages
        ));
    }

    @GetMapping("/chat/sessions")
    public Result<List<ChatSessionResponse>> listSessions() {
        return Result.success(conversationMemoryService.listSessions());
    }
}
