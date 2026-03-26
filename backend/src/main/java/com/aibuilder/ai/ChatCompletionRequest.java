package com.aibuilder.ai;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        double temperature
) {
    public record Message(String role, String content) {
    }
}
