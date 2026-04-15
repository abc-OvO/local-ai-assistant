package com.example.localai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaGenerateResponse {

    private String model;

    private String response;

    private Boolean done;
}
