package com.example.localai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class KimiRestClientConfig {

    @Bean
    public RestClient kimiRestClient(KimiProperties kimiProperties) {
        return RestClient.builder()
                .baseUrl(kimiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
