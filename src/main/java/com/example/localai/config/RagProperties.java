package com.example.localai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Rewrite rewrite = new Rewrite();

    private Hybrid hybrid = new Hybrid();

    private Mmr mmr = new Mmr();

    private Rerank rerank = new Rerank();

    @Data
    public static class Rewrite {

        private boolean enabled = true;

        @Min(value = 20, message = "rag.rewrite.max-length 不能小于 20")
        private Integer maxLength = 160;
    }

    @Data
    public static class Hybrid {

        private boolean enabled = true;

        @DecimalMin(value = "0.0", message = "rag.hybrid.vector-weight 不能小于 0")
        private Double vectorWeight = 1.0;

        @DecimalMin(value = "0.0", message = "rag.hybrid.keyword-weight 不能小于 0")
        private Double keywordWeight = 0.35;
    }

    @Data
    public static class Mmr {

        private boolean enabled = true;

        @DecimalMin(value = "0.0", message = "rag.mmr.lambda 不能小于 0")
        @DecimalMax(value = "1.0", message = "rag.mmr.lambda 不能大于 1")
        private Double lambda = 0.75;

        @Min(value = 1, message = "rag.mmr.candidate-size 不能小于 1")
        private Integer candidateSize = 12;
    }

    @Data
    public static class Rerank {

        private boolean enabled = false;
    }
}
