package com.example.localai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 支持 ollama / kimi
     */
    private String provider = "ollama";
}
