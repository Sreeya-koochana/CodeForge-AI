package com.aibuilder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GenerateApiRequest(
        @NotNull Long projectId,
        @NotBlank String entityName,
        @NotEmpty List<@Valid FieldDefinitionRequest> fields,
        String description,
        boolean includeJwt,
        boolean multiEntity,
        String rawPrompt
) {
}
