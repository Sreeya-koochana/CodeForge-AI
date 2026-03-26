package com.aibuilder.dto;

import jakarta.validation.constraints.NotBlank;

public record FieldDefinitionRequest(
        @NotBlank String name,
        @NotBlank String type,
        boolean required
) {
}
