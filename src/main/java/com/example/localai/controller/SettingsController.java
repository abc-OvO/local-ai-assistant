package com.example.localai.controller;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.common.Result;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.ProviderStatusResponse;
import com.example.localai.dto.ProviderSwitchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final AiChatClientRouter aiChatClientRouter;

    private final OllamaProperties ollamaProperties;

    @GetMapping("/provider")
    public Result<ProviderStatusResponse> getProvider() {
        return Result.success(toResponse());
    }

    @PostMapping("/provider")
    public Result<ProviderStatusResponse> switchProvider(@Valid @RequestBody ProviderSwitchRequest request) {
        aiChatClientRouter.switchProvider(request.getProvider());
        return Result.success(toResponse());
    }

    private ProviderStatusResponse toResponse() {
        String provider = aiChatClientRouter.currentProvider();
        return new ProviderStatusResponse(
                provider,
                List.of("kimi", "ollama"),
                provider,
                "ollama",
                ollamaProperties.getEmbeddingModel()
        );
    }
}
