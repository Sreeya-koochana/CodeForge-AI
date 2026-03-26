package com.aibuilder.dto;

import java.util.Map;

public record ApiTestResponse(
        int status,
        Map<String, String> headers,
        String body
) {
}
