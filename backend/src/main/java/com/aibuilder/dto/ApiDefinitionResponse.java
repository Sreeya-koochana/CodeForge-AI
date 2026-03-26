package com.aibuilder.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiDefinitionResponse(
        Long id,
        Long projectId,
        String prompt,
        String generatedCode,
        LocalDateTime createdAt,
        List<GeneratedFileResponse> files
) {
}
