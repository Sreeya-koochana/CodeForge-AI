package com.aibuilder.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotBlank String baseUrl,
        double temperature
) {
}
