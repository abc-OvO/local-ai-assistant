package com.example.localai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OllamaGenerateRequest {

    private String model;

    private String prompt;

    /**
     * false 表示使用 Ollama 非流式响应，方便后端一次性返回完整回答。
     */
    private Boolean stream;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Options options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {

        @JsonProperty("num_predict")
        private Integer numPredict;
    }
}
