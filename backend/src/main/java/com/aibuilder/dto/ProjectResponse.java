package com.aibuilder.dto;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long userId,
        String ownerName,
        LocalDateTime createdAt,
        long apiCount,
        long fileCount
) {
}
