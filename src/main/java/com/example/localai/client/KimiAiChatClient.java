package com.example.localai.client;

import com.example.localai.config.KimiProperties;
import com.example.localai.dto.KimiChatCompletionRequest;
import com.example.localai.dto.KimiChatCompletionResponse;
import com.example.localai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KimiAiChatClient implements AiChatClient {

    private final RestClient kimiRestClient;

    private final KimiProperties kimiProperties;

    @Override
    public String generate(String prompt) {
        if (!StringUtils.hasText(kimiProperties.getApiKey())) {
            throw new BusinessException(500, "Kimi API Key 未配置，请设置 kimi.api-key 或环境变量 MOONSHOT_API_KEY");
        }

        KimiChatCompletionRequest request = KimiChatCompletionRequest.of(kimiProperties.getModel(), prompt);

        long start = System.currentTimeMillis();
        System.out.println("[KimiAiChatClient] request start, endpoint=" + kimiProperties.getBaseUrl() + "/chat/completions"
                + ", model=" + kimiProperties.getModel()
                + ", promptLength=" + prompt.length());

        try {
            KimiChatCompletionResponse response = kimiRestClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kimiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(502, "Kimi 调用失败，HTTP 状态码：" + res.getStatusCode().value());
                    })
                    .body(KimiChatCompletionResponse.class);

            long cost = System.currentTimeMillis() - start;
            System.out.println("[KimiAiChatClient] request success, costMs=" + cost);

            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null
                    || !StringUtils.hasText(response.getChoices().get(0).getMessage().getContent())) {
                throw new BusinessException(502, "Kimi 返回内容为空");
            }
            return response.getChoices().get(0).getMessage().getContent();
        } catch (ResourceAccessException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[KimiAiChatClient] request failed, costMs=" + cost + ", error=" + ex.getMessage());
            throw new BusinessException(502, "Kimi 服务不可用，请检查网络或 API 地址配置", ex);
        } catch (RestClientException ex) {
            long cost = System.currentTimeMillis() - start;
            System.out.println("[KimiAiChatClient] request failed, costMs=" + cost + ", error=" + ex.getMessage());
            throw new BusinessException(502, "调用 Kimi 服务失败：" + ex.getMessage(), ex);
        }
    }
}
