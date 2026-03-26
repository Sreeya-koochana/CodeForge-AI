package com.aibuilder.ai;

import com.aibuilder.config.OpenAiProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiChatCompletionsClient implements OpenAiClient {

    private static final String SYSTEM_PROMPT = """
            You are a senior Java Spring Boot architect.
            Return production-quality code for a complete Spring Boot REST API.
            Separate every file with its filename on a single line, then wrap file contents in fenced code blocks.
            Use Java 17, Maven, Spring Boot, JPA, Hibernate, validation, DTOs, Lombok, and clean layering.
            """;

    private final RestClient restClient;
    private final OpenAiProperties properties;

    public OpenAiChatCompletionsClient(RestClient openAiRestClient, OpenAiProperties properties) {
        this.restClient = openAiRestClient;
        this.properties = properties;
    }

    @Override
    public String generateCode(String prompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank() || "replace-me".equals(properties.apiKey())) {
            throw new IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY or app.openai.api-key before generating code.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(
                        new ChatCompletionRequest.Message("system", SYSTEM_PROMPT),
                        new ChatCompletionRequest.Message("user", prompt)
                ),
                properties.temperature()
        );

        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new IllegalStateException("OpenAI rejected the API key. Please update OPENAI_API_KEY with a valid key and try again.");
            }
            throw new IllegalStateException("OpenAI request failed: " + exception.getStatusText(), exception);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }

        return response.choices().get(0).message().content();
    }
}
