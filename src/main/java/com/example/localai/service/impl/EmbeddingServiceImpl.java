package com.example.localai.service.impl;

import com.example.localai.client.OllamaClient;
import com.example.localai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final OllamaClient ollamaClient;

    @Override
    public List<Double> embed(String text) {
        return ollamaClient.embedding(text);
    }
}
