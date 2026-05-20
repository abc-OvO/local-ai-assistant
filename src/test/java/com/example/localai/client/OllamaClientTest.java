package com.example.localai.client;

import com.example.localai.config.OllamaProperties;
import com.example.localai.dto.OllamaGenerateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaClientTest {

    @Test
    void generateSendsExpectedOllamaRequestBody() {
        TestContext context = newTestContext();
        context.server.expect(once(), requestTo("http://localhost:11434/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "model": "qwen2.5:0.5b",
                          "prompt": "hello",
                          "stream": false,
                          "options": {
                            "num_predict": 128
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {"model":"qwen2.5:0.5b","response":"local reply","done":true}
                        """, MediaType.APPLICATION_JSON));

        OllamaGenerateResponse response = context.client.generate("hello");

        assertThat(response.getResponse()).isEqualTo("local reply");
        context.server.verify();
    }

    @Test
    void embeddingUsesLocalOllamaEmbeddingModel() {
        TestContext context = newTestContext();
        context.server.expect(once(), requestTo("http://localhost:11434/api/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "model": "nomic-embed-text",
                          "prompt": "hello"
                        }
                        """))
                .andRespond(withSuccess("""
                        {"embedding":[0.1,0.2,0.3]}
                        """, MediaType.APPLICATION_JSON));

        List<Double> embedding = context.client.embedding("hello");

        assertThat(embedding).containsExactly(0.1, 0.2, 0.3);
        context.server.verify();
    }

    private TestContext newTestContext() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        OllamaProperties properties = new OllamaProperties();
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("qwen2.5:0.5b");
        properties.setEmbeddingModel("nomic-embed-text");
        properties.setGenerateNumPredict(128);

        return new TestContext(server, new OllamaClient(restClient, properties));
    }

    private record TestContext(MockRestServiceServer server, OllamaClient client) {
    }
}
