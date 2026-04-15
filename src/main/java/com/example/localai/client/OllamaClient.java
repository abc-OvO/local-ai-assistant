package com.example.localai.client;

import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.OllamaGenerateRequest;
import com.example.localai.dto.OllamaGenerateResponse;
import com.example.localai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

        try {
            OllamaGenerateResponse response = ollamaRestClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(502, "Ollama 调用失败，HTTP 状态码：" + res.getStatusCode().value());
                    })
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.getResponse() == null) {
                throw new BusinessException(502, "Ollama 返回内容为空");
            }
            return response;
        } catch (ResourceAccessException ex) {
            throw new BusinessException(502, "Ollama 服务不可用，请检查地址、网络或 Tailscale 子网路由", ex);
        } catch (RestClientException ex) {
            throw new BusinessException(502, "调用 Ollama 服务失败：" + ex.getMessage(), ex);
        }
    }
}
