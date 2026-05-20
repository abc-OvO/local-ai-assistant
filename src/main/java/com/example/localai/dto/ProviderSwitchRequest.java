package com.example.localai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderSwitchRequest {

    @NotBlank(message = "provider 不能为空")
    private String provider;
}
