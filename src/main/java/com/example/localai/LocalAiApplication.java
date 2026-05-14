package com.example.localai;

import com.example.localai.config.AiProperties;
import com.example.localai.config.AppProperties;
import com.example.localai.config.KimiProperties;
import com.example.localai.config.OllamaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({OllamaProperties.class, AppProperties.class, AiProperties.class, KimiProperties.class})
public class LocalAiApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(LocalAiApplication.class, args);
    }

    private static void loadDotenv() {
        Path dotenvPath = Path.of(".env");
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(dotenvPath);
            for (String line : lines) {
                loadDotenvLine(line);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("读取 .env 文件失败：" + ex.getMessage(), ex);
        }
    }

    private static void loadDotenvLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }

        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return;
        }

        String key = trimmed.substring(0, separatorIndex).trim();
        String value = stripDotenvQuotes(trimmed.substring(separatorIndex + 1).trim());
        if (key.isEmpty() || System.getProperty(key) != null || System.getenv(key) != null) {
            return;
        }

        System.setProperty(key, value);
    }

    private static String stripDotenvQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
