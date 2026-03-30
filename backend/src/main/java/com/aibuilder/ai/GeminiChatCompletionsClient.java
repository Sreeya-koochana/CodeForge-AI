package com.aibuilder.ai;

import com.aibuilder.config.GeminiProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiChatCompletionsClient implements GeminiClient {

    private static final String SYSTEM_PROMPT = """
            You are a senior Java Spring Boot architect.
            Return production-quality code for a complete Spring Boot REST API.
            Separate every file with its filename on a single line, then wrap file contents in fenced code blocks.
            Use Java 17, Maven, Spring Boot, JPA, Hibernate, validation, DTOs, Lombok, and clean layering.
            """;

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiChatCompletionsClient(RestClient geminiRestClient, GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.properties = properties;
    }

    @Override
    public String generateCode(String prompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank() || "replace-me".equals(properties.apiKey())) {
            throw new IllegalStateException("Gemini API key is not configured. Set GEMINI_API_KEY, GOOGLE_API_KEY, or app.gemini.api-key before generating code.");
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
                throw new IllegalStateException("Gemini rejected the API key. Please update GEMINI_API_KEY or GOOGLE_API_KEY with a valid key and try again.");
            }
            throw new IllegalStateException("Gemini request failed: " + exception.getStatusText(), exception);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Gemini returned an empty response.");
        }

        return response.choices().get(0).message().content();
    }
}
