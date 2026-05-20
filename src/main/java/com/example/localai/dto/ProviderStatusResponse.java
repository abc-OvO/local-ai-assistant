package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatusResponse {

    private String currentProvider;

    private List<String> availableProviders;

    private String generationProvider;

    private String embeddingProvider;

    private String embeddingModel;
}
