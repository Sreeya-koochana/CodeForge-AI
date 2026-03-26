package com.aibuilder.service;

import com.aibuilder.dto.FieldDefinitionRequest;
import com.aibuilder.dto.GenerateApiRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    public String buildPrompt(GenerateApiRequest request) {
        String fields = request.fields().stream()
                .map(this::formatField)
                .collect(Collectors.joining(", "));

        StringBuilder prompt = new StringBuilder("""
                Act as a senior Java Spring Boot architect.

                Generate a complete Spring Boot REST API project using:
                - Java 17
                - Spring Boot
                - Maven
                - MySQL (JPA + Hibernate)

                Requirements:
                1. Entity name: %s
                2. Fields: %s
                3. Include:
                   - Entity class with JPA annotations
                   - Repository interface (JpaRepository)
                   - Service layer (interface + implementation)
                   - REST Controller
                   - DTO classes
                4. Include CRUD operations:
                   - Create
                   - Read
                   - Update
                   - Delete
                5. Use clean architecture and best practices
                6. Add validation annotations
                7. Add proper package structure
                8. Use Lombok where needed
                """.formatted(request.entityName(), fields));

        if (request.includeJwt()) {
            prompt.append("9. Also implement JWT authentication with login and register endpoints.\n");
        }
        if (request.multiEntity()) {
            prompt.append("10. Model related entities and relationships cleanly when needed.\n");
        }

        prompt.append("""

                Additional context:
                - Functional description: %s
                - Raw user request: %s

                Output format:
                - Separate each file with filename
                - Provide complete code
                """.formatted(blankIfNull(request.description()), blankIfNull(request.rawPrompt())));

        return prompt.toString();
    }

    private String formatField(FieldDefinitionRequest field) {
        String requiredFlag = field.required() ? "required" : "optional";
        return "%s(%s, %s)".formatted(field.name(), field.type(), requiredFlag);
    }

    private String blankIfNull(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.trim();
    }
}
