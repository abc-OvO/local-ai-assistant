package com.example.localai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank(message = "upload-dir 不能为空")
    private String uploadDir = "data/uploads";

    @Min(value = 1000, message = "max-context-length 不能小于 1000")
    private Integer maxContextLength = 4000;

    @Min(value = 100, message = "chunk-size 不能小于 100")
    private Integer chunkSize = 400;

    @Min(value = 0, message = "chunk-overlap 不能小于 0")
    private Integer chunkOverlap = 80;

    @Min(value = 1, message = "retrieval-top-k 不能小于 1")
    private Integer retrievalTopK = 3;

    private Memory memory = new Memory();

    @Data
    public static class Memory {

        private boolean enabled = true;

        @Min(value = 0, message = "memory.max-turns 不能小于 0")
        private Integer maxTurns = 6;
    }
}
