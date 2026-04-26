package com.example.localai.service.impl;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.dto.ChatResponse;
import com.example.localai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatClientRouter aiChatClientRouter;

    @Override
    public ChatResponse chat(String message) {
        String reply = aiChatClientRouter.generate(message);
        return new ChatResponse(aiChatClientRouter.currentModel(), reply);
    }
}
