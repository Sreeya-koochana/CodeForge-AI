package com.aibuilder.dto;

import java.util.Map;

public record ApiTestRequest(
        String url,
        String method,
        Map<String, String> headers,
        String body
) {
}
