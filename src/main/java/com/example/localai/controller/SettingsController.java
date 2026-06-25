package com.example.localai.controller;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.common.Result;
import com.example.localai.config.KimiProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.ModelStatusResponse;
import com.example.localai.dto.ModelSwitchRequest;
import com.example.localai.dto.ProviderStatusResponse;
import com.example.localai.dto.ProviderSwitchRequest;
import com.example.localai.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final AiChatClientRouter aiChatClientRouter;

    private final OllamaProperties ollamaProperties;

    private final KimiProperties kimiProperties;

    @GetMapping("/provider")
    public Result<ProviderStatusResponse> getProvider() {
        return Result.success(toResponse());
    }

    @PostMapping("/provider")
    public Result<ProviderStatusResponse> switchProvider(@Valid @RequestBody ProviderSwitchRequest request) {
        aiChatClientRouter.switchProvider(request.getProvider());
        return Result.success(toResponse());
    }

    @GetMapping("/model")
    public Result<ModelStatusResponse> getModel() {
        return Result.success(toModelResponse());
    }

    @PostMapping("/model")
    public Result<ModelStatusResponse> switchModel(@Valid @RequestBody ModelSwitchRequest request) {
        String requestedModel = request.getModel().trim().toLowerCase(Locale.ROOT);
        boolean supported = kimiProperties.getAvailableModels().stream()
                .map(model -> model.toLowerCase(Locale.ROOT))
                .anyMatch(requestedModel::equals);
        if (!supported) {
            throw new BusinessException(
                    400,
                    "不支持的 Kimi 模型：" + request.getModel()
            );
        }
        kimiProperties.setModel(requestedModel);
        aiChatClientRouter.switchProvider("kimi");
        return Result.success(toModelResponse());
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

    private ModelStatusResponse toModelResponse() {
        return new ModelStatusResponse(
                kimiProperties.getModel(),
                List.copyOf(kimiProperties.getAvailableModels()),
                ollamaProperties.getEmbeddingModel()
        );
    }
}
