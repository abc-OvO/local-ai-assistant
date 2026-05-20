package com.example.localai.service;

import com.example.localai.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmbeddingJsonCodec {

    private static final TypeReference<List<Double>> DOUBLE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "embedding 序列化失败：" + ex.getMessage(), ex);
        }
    }

    public List<Double> fromJson(String embeddingJson) {
        try {
            return objectMapper.readValue(embeddingJson, DOUBLE_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "embedding 解析失败：" + ex.getMessage(), ex);
        }
    }
}
