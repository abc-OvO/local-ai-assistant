package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KimiChatCompletionRequest {

    private String model;

    private List<Message> messages;

    public static KimiChatCompletionRequest of(String model, String prompt) {
        return new KimiChatCompletionRequest(
                model,
                List.of(new Message("user", prompt))
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        private String role;

        private String content;
    }
}
