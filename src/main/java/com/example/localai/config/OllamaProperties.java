package com.example.localai.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    /**
     * 本地 Ollama 地址。新架构中主要用于 embedding。
     */
    private String baseUrl = "http://localhost:11434";

    @NotBlank(message = "Ollama model 不能为空")
    private String model = "qwen2.5:0.5b";

    @NotBlank(message = "Ollama embedding-model 不能为空")
    private String embeddingModel = "nomic-embed-text";

    /**
     * 限制 /api/generate 最大生成 token 数，避免大模型非流式生成时间过长。
     */
    private Integer generateNumPredict = 128;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectTimeout = Duration.ofSeconds(5);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration readTimeout = Duration.ofSeconds(60);

    public String effectiveBaseUrl() {
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        return "http://localhost:11434";
    }
}
