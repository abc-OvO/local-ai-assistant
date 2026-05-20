package com.example.localai.client;

import com.example.localai.config.KimiProperties;
import com.example.localai.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KimiAiChatClientTest {

    @Test
    void generateFailsClearlyWhenApiKeyMissing() {
        KimiProperties properties = new KimiProperties();
        properties.setBaseUrl("https://api.moonshot.ai/v1");
        properties.setModel("kimi-k2.6");
        properties.setApiKey("");

        KimiAiChatClient client = new KimiAiChatClient(RestClient.builder().build(), properties);

        assertThatThrownBy(() -> client.generate("hello"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MOONSHOT_API_KEY");
    }
}
