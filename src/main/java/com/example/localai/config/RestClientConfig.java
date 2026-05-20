package com.example.localai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient ollamaRestClient(OllamaProperties properties) {
        System.out.println("[RestClientConfig] Ollama RestClient timeout config"
                + ", baseUrl=" + properties.effectiveBaseUrl()
                + ", connectTimeout=" + properties.getConnectTimeout()
                + ", readTimeout=" + properties.getReadTimeout());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.effectiveBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
