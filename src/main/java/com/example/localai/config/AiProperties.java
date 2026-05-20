package com.example.localai.config;

import com.example.localai.exception.BusinessException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 支持 ollama / kimi
     */
    private String provider = "ollama";

    public String normalizedProvider() {
        return provider == null ? "ollama" : provider.trim().toLowerCase(Locale.ROOT);
    }

    public void switchProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!"kimi".equals(normalized) && !"ollama".equals(normalized)) {
            throw new BusinessException(400, "不支持的 generation provider：" + provider + "，仅支持 kimi / ollama");
        }
        this.provider = normalized;
    }
}
