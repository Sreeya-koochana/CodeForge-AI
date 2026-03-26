package com.aibuilder.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiGenerationProperties(
        @NotBlank String provider
) {
}
