package com.example.localai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelSwitchRequest {

    @NotBlank(message = "model 不能为空")
    private String model;
}
