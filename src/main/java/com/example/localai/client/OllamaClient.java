package com.example.localai.client;

import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.OllamaEmbeddingRequest;
import com.example.localai.dto.OllamaEmbeddingResponse;
import com.example.localai.dto.OllamaGenerateRequest;
import com.example.localai.dto.OllamaGenerateResponse;
import com.example.localai.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OllamaClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient ollamaRestClient;

    private final OllamaProperties properties;

    public OllamaGenerateResponse generate(String message) {
        OllamaGenerateRequest request = new OllamaGenerateRequest(
                properties.getModel(),
                message,
                false,
                buildGenerateOptions()
        );

        String endpoint = properties.effectiveBaseUrl();
        long start = System.currentTimeMillis();
        System.out.println("[OllamaClient] generate try, endpoint=" + endpoint
                + ", model=" + properties.getModel()
                + ", stream=" + request.getStream()
                + ", promptLength=" + message.length()
                + ", options=" + summarizeOptions(request.getOptions()));

        try {
            OllamaGenerateResponse response = ollamaRestClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        HttpStatusCode statusCode = clientResponse.getStatusCode();
                        if (statusCode.isError()) {
                            String errorBody = readBody(clientResponse);
                            throw new BusinessException(502, "Ollama generate 调用失败，HTTP 状态码："
                                    + statusCode.value() + "，错误信息：" + errorBody);
                        }
                        return readGenerateResponse(clientResponse);
                    });

            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] generate success, endpoint=" + endpoint
                    + ", promptLength=" + message.length()
                    + ", costMs=" + cost);

            if (response == null || !StringUtils.hasText(response.getResponse())) {
                throw new BusinessException(502, "Ollama generate 返回内容为空");
            }
            return response;
        } catch (ResourceAccessException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] generate failed, endpoint=" + endpoint
                    + ", promptLength=" + message.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "Ollama generate 服务不可用，请检查本地 Ollama 是否启动：" + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] generate failed, endpoint=" + endpoint
                    + ", promptLength=" + message.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "调用 Ollama generate 服务失败：" + ex.getMessage(), ex);
        }
    }

    public List<Double> embedding(String text) {
        OllamaEmbeddingRequest request = new OllamaEmbeddingRequest(
                properties.getEmbeddingModel(),
                text
        );

        String endpoint = properties.effectiveBaseUrl();
        long start = System.currentTimeMillis();
        System.out.println("[OllamaClient] embedding try, provider=ollama"
                + ", endpoint=" + endpoint
                + ", model=" + properties.getEmbeddingModel()
                + ", textLength=" + text.length());

        try {
            OllamaEmbeddingResponse response = ollamaRestClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((clientRequest, clientResponse) -> {
                        HttpStatusCode statusCode = clientResponse.getStatusCode();
                        if (statusCode.isError()) {
                            String errorBody = readBody(clientResponse);
                            throw new BusinessException(502, "Ollama embedding 调用失败，HTTP 状态码："
                                    + statusCode.value() + "，错误信息：" + errorBody);
                        }
                        return readEmbeddingResponse(clientResponse);
                    });

            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding success, provider=ollama"
                    + ", endpoint=" + endpoint
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost);

            if (response == null || response.getEmbedding() == null || response.getEmbedding().isEmpty()) {
                throw new BusinessException(502, "Ollama embedding 返回内容为空");
            }
            return response.getEmbedding();
        } catch (ResourceAccessException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding failed, provider=ollama"
                    + ", endpoint=" + endpoint
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "Ollama embedding 服务不可用，请检查本地 Ollama 是否启动：" + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding failed, provider=ollama"
                    + ", endpoint=" + endpoint
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "调用 Ollama embedding 服务失败：" + ex.getMessage(), ex);
        }
    }

    private OllamaGenerateRequest.Options buildGenerateOptions() {
        if (properties.getGenerateNumPredict() == null) {
            return null;
        }
        return new OllamaGenerateRequest.Options(properties.getGenerateNumPredict());
    }

    private String summarizeOptions(OllamaGenerateRequest.Options options) {
        if (options == null) {
            return "{}";
        }
        return "{num_predict=" + options.getNumPredict() + "}";
    }

    private OllamaGenerateResponse readGenerateResponse(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bodyBytes = response.getBody().readAllBytes();
            if (bodyBytes.length == 0) {
                return null;
            }
            return OBJECT_MAPPER.readValue(bodyBytes, OllamaGenerateResponse.class);
        } catch (java.io.IOException ex) {
            throw new BusinessException(502, "解析 Ollama generate 响应失败：" + ex.getMessage(), ex);
        }
    }

    private OllamaEmbeddingResponse readEmbeddingResponse(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bodyBytes = response.getBody().readAllBytes();
            if (bodyBytes.length == 0) {
                return null;
            }
            return OBJECT_MAPPER.readValue(bodyBytes, OllamaEmbeddingResponse.class);
        } catch (java.io.IOException ex) {
            throw new BusinessException(502, "解析 Ollama embedding 响应失败：" + ex.getMessage(), ex);
        }
    }

    private String readBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bodyBytes = response.getBody().readAllBytes();
            if (bodyBytes.length == 0) {
                return "<empty>";
            }
            return new String(bodyBytes, StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            return "<unreadable: " + ex.getMessage() + ">";
        }
    }
}
