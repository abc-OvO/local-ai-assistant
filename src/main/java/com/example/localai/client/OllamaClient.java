package com.example.localai.client;

import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.OllamaEmbeddingRequest;
import com.example.localai.dto.OllamaEmbeddingResponse;
import com.example.localai.dto.OllamaGenerateRequest;
import com.example.localai.dto.OllamaGenerateResponse;
import com.example.localai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    public OllamaGenerateResponse generate(String message) {
        OllamaGenerateRequest request = new OllamaGenerateRequest(
                properties.getModel(),
                message,
                false
        );

        long start = System.currentTimeMillis();
        System.out.println("[OllamaClient] request start, promptLength=" + message.length());

        try {
            OllamaGenerateResponse response = ollamaRestClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(502, "Ollama 调用失败，HTTP 状态码：" + res.getStatusCode().value());
                    })
                    .body(OllamaGenerateResponse.class);

            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] request success, costMs=" + cost);

            if (response == null || !org.springframework.util.StringUtils.hasText(response.getResponse())) {
                throw new BusinessException(502, "Ollama 返回内容为空");
            }
            return response;
        } catch (ResourceAccessException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] request failed, costMs=" + cost + ", error=" + ex.getMessage());

            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof java.net.SocketTimeoutException) {
                    throw new BusinessException(502, "Ollama 响应超时，请稍后重试或适当增大 read-timeout", ex);
                }
                cause = cause.getCause();
            }

            throw new BusinessException(502, "Ollama 服务不可用，请检查地址、网络或 Tailscale 子网路由", ex);
        } catch (RestClientException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] request failed, costMs=" + cost + ", error=" + ex.getMessage());
            throw new BusinessException(502, "调用 Ollama 服务失败：" + ex.getMessage(), ex);
        }
    }

    public List<Double> embedding(String text) {
        OllamaEmbeddingRequest request = new OllamaEmbeddingRequest(
                properties.getEmbeddingModel(),
                text
        );

        long start = System.currentTimeMillis();
        System.out.println("[OllamaClient] embedding start, model=" + properties.getEmbeddingModel()
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
                            System.out.println("[OllamaClient] embedding failed, model=" + properties.getEmbeddingModel()
                                    + ", textLength=" + text.length()
                                    + ", httpStatus=" + statusCode.value()
                                    + ", errorBody=" + errorBody);
                            throw new BusinessException(502, "Ollama embedding 调用失败，HTTP 状态码："
                                    + statusCode.value() + "，错误信息：" + errorBody);
                        }
                        return readEmbeddingResponse(clientResponse);
                    });

            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding success, model=" + properties.getEmbeddingModel()
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost);

            if (response == null || response.getEmbedding() == null || response.getEmbedding().isEmpty()) {
                throw new BusinessException(502, "Ollama embedding 返回内容为空");
            }
            return response.getEmbedding();
        } catch (ResourceAccessException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding failed, model=" + properties.getEmbeddingModel()
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "Ollama embedding 服务不可用，请检查地址、网络或 Tailscale 子网路由", ex);
        } catch (RestClientException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[OllamaClient] embedding failed, model=" + properties.getEmbeddingModel()
                    + ", textLength=" + text.length()
                    + ", costMs=" + cost
                    + ", error=" + ex.getMessage());
            throw new BusinessException(502, "调用 Ollama embedding 服务失败：" + ex.getMessage(), ex);
        }
    }

    private OllamaEmbeddingResponse readEmbeddingResponse(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bodyBytes = response.getBody().readAllBytes();
            if (bodyBytes.length == 0) {
                return null;
            }
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(bodyBytes, OllamaEmbeddingResponse.class);
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
