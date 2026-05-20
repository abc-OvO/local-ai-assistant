package com.example.localai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kimi")
public class KimiProperties {

    /**
     * Kimi 官方 OpenAI 兼容接口基地址。
     */
    private String baseUrl = "https://api.moonshot.ai/v1";

    /**
     * 建议通过环境变量 MOONSHOT_API_KEY 注入。
     */
    private String apiKey;

    private String model = "kimi-k2.6";
}
