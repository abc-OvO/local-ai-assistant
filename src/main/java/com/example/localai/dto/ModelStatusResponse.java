package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatusResponse {

    private String currentModel;

    private List<String> availableModels;

    private String embeddingModel;
}
