package com.example.localai;

import com.example.localai.config.AppProperties;
import com.example.localai.config.OllamaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OllamaProperties.class, AppProperties.class})
public class LocalAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalAiApplication.class, args);
    }
}
