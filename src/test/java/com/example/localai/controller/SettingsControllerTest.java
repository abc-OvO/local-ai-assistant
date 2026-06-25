package com.example.localai.controller;

import com.example.localai.client.AiChatClientRouter;
import com.example.localai.config.KimiProperties;
import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.ModelStatusResponse;
import com.example.localai.dto.ModelSwitchRequest;
import com.example.localai.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SettingsControllerTest {

    @Test
    void switchesConfiguredKimiModel() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        OllamaProperties ollamaProperties = new OllamaProperties();
        KimiProperties kimiProperties = new KimiProperties();
        kimiProperties.setAvailableModels(List.of("kimi-k2.6", "kimi-k2.5"));
        SettingsController controller = new SettingsController(router, ollamaProperties, kimiProperties);

        ModelSwitchRequest request = new ModelSwitchRequest();
        request.setModel("kimi-k2.5");

        ModelStatusResponse response = controller.switchModel(request).getData();

        assertEquals("kimi-k2.5", response.getCurrentModel());
        assertEquals(List.of("kimi-k2.6", "kimi-k2.5"), response.getAvailableModels());
        assertEquals("nomic-embed-text", response.getEmbeddingModel());
        verify(router).switchProvider("kimi");
    }

    @Test
    void rejectsModelOutsideConfiguredList() {
        AiChatClientRouter router = mock(AiChatClientRouter.class);
        OllamaProperties ollamaProperties = new OllamaProperties();
        KimiProperties kimiProperties = new KimiProperties();
        SettingsController controller = new SettingsController(router, ollamaProperties, kimiProperties);

        ModelSwitchRequest request = new ModelSwitchRequest();
        request.setModel("unknown-model");

        assertThrows(BusinessException.class, () -> controller.switchModel(request));
    }
}
