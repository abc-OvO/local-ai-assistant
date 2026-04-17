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
}
