package com.aibuilder.service;

import com.aibuilder.dto.FieldDefinitionRequest;
import com.aibuilder.dto.GenerateApiRequest;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LocalTemplateGenerationService {

    public String generate(GenerateApiRequest request) {
        String entity = capitalize(request.entityName().trim());
        String variable = decapitalize(entity);
        String packageBase = "com.generated." + variable;
        String tableName = variable + "s";

        String entityFields = request.fields().stream()
                .map(this::entityField)
                .collect(Collectors.joining("\n\n"));

        String requestFields = request.fields().stream()
                .filter(field -> !"id".equalsIgnoreCase(field.name()))
                .map(this::dtoField)
                .collect(Collectors.joining("\n\n"));

        String responseFields = request.fields().stream()
                .map(field -> "    private " + normalizeType(field.type()) + " " + field.name() + ";")
                .collect(Collectors.joining("\n\n"));

        String builderAssignments = request.fields().stream()
                .filter(field -> !"id".equalsIgnoreCase(field.name()))
                .map(field -> "                ." + field.name() + "(request.get" + capitalize(field.name()) + "())")
                .collect(Collectors.joining("\n"));

        String updateAssignments = request.fields().stream()
                .filter(field -> !"id".equalsIgnoreCase(field.name()))
                .map(field -> "        existing.set" + capitalize(field.name()) + "(request.get" + capitalize(field.name()) + "());")
                .collect(Collectors.joining("\n"));

        String responseAssignments = request.fields().stream()
                .map(field -> "                ." + field.name() + "(" + variable + ".get" + capitalize(field.name()) + "())")
                .collect(Collectors.joining("\n"));

        return file(entity + ".java", entityClass(packageBase, entity, tableName, entityFields))
                + file(entity + "Request.java", requestDto(packageBase, entity, requestFields))
                + file(entity + "Response.java", responseDto(packageBase, entity, responseFields))
                + file(entity + "Repository.java", repository(packageBase, entity))
                + file(entity + "Service.java", service(packageBase, entity))
                + file(entity + "ServiceImpl.java", serviceImpl(packageBase, entity, variable, builderAssignments, updateAssignments, responseAssignments))
                + file(entity + "Controller.java", controller(packageBase, entity, variable, tableName));
    }

    private String file(String fileName, String content) {
        return fileName + "\n```java\n" + content + "\n```\n";
    }

    private String entityClass(String packageBase, String entity, String tableName, String entityFields) {
        return "package " + packageBase + ".entity;\n\n"
                + "import jakarta.persistence.*;\n"
                + "import lombok.*;\n\n"
                + "@Entity\n"
                + "@Table(name = \"" + tableName + "\")\n"
                + "@Getter\n"
                + "@Setter\n"
                + "@NoArgsConstructor\n"
                + "@AllArgsConstructor\n"
                + "@Builder\n"
                + "public class " + entity + " {\n\n"
                + entityFields + "\n"
                + "}\n";
    }

    private String requestDto(String packageBase, String entity, String requestFields) {
        return "package " + packageBase + ".dto;\n\n"
                + "import jakarta.validation.constraints.*;\n"
                + "import lombok.*;\n\n"
                + "@Getter\n"
                + "@Setter\n"
                + "@NoArgsConstructor\n"
                + "@AllArgsConstructor\n"
                + "@Builder\n"
                + "public class " + entity + "Request {\n\n"
                + requestFields + "\n"
                + "}\n";
    }

    private String responseDto(String packageBase, String entity, String responseFields) {
        return "package " + packageBase + ".dto;\n\n"
                + "import lombok.*;\n\n"
                + "@Getter\n"
                + "@Setter\n"
                + "@NoArgsConstructor\n"
                + "@AllArgsConstructor\n"
                + "@Builder\n"
                + "public class " + entity + "Response {\n\n"
                + responseFields + "\n"
                + "}\n";
    }

    private String repository(String packageBase, String entity) {
        return "package " + packageBase + ".repository;\n\n"
                + "import " + packageBase + ".entity." + entity + ";\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n\n"
                + "public interface " + entity + "Repository extends JpaRepository<" + entity + ", Long> {\n"
                + "}\n";
    }

    private String service(String packageBase, String entity) {
        return "package " + packageBase + ".service;\n\n"
                + "import " + packageBase + ".dto." + entity + "Request;\n"
                + "import " + packageBase + ".dto." + entity + "Response;\n"
                + "import java.util.List;\n\n"
                + "public interface " + entity + "Service {\n"
                + "    " + entity + "Response create(" + entity + "Request request);\n"
                + "    List<" + entity + "Response> getAll();\n"
                + "    " + entity + "Response getById(Long id);\n"
                + "    " + entity + "Response update(Long id, " + entity + "Request request);\n"
                + "    void delete(Long id);\n"
                + "}\n";
    }

    private String serviceImpl(
            String packageBase,
            String entity,
            String variable,
            String builderAssignments,
            String updateAssignments,
            String responseAssignments
    ) {
        return "package " + packageBase + ".service;\n\n"
                + "import " + packageBase + ".dto." + entity + "Request;\n"
                + "import " + packageBase + ".dto." + entity + "Response;\n"
                + "import " + packageBase + ".entity." + entity + ";\n"
                + "import " + packageBase + ".repository." + entity + "Repository;\n"
                + "import java.util.List;\n"
                + "import org.springframework.stereotype.Service;\n\n"
                + "@Service\n"
                + "public class " + entity + "ServiceImpl implements " + entity + "Service {\n\n"
                + "    private final " + entity + "Repository " + variable + "Repository;\n\n"
                + "    public " + entity + "ServiceImpl(" + entity + "Repository " + variable + "Repository) {\n"
                + "        this." + variable + "Repository = " + variable + "Repository;\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public " + entity + "Response create(" + entity + "Request request) {\n"
                + "        " + entity + " " + variable + " = " + entity + ".builder()\n"
                + builderAssignments + "\n"
                + "                .build();\n\n"
                + "        return toResponse(" + variable + "Repository.save(" + variable + "));\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public List<" + entity + "Response> getAll() {\n"
                + "        return " + variable + "Repository.findAll().stream().map(this::toResponse).toList();\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public " + entity + "Response getById(Long id) {\n"
                + "        return toResponse(" + variable + "Repository.findById(id)\n"
                + "                .orElseThrow(() -> new IllegalArgumentException(\"" + entity + " not found: \" + id)));\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public " + entity + "Response update(Long id, " + entity + "Request request) {\n"
                + "        " + entity + " existing = " + variable + "Repository.findById(id)\n"
                + "                .orElseThrow(() -> new IllegalArgumentException(\"" + entity + " not found: \" + id));\n\n"
                + updateAssignments + "\n\n"
                + "        return toResponse(" + variable + "Repository.save(existing));\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public void delete(Long id) {\n"
                + "        " + variable + "Repository.deleteById(id);\n"
                + "    }\n\n"
                + "    private " + entity + "Response toResponse(" + entity + " " + variable + ") {\n"
                + "        return " + entity + "Response.builder()\n"
                + responseAssignments + "\n"
                + "                .build();\n"
                + "    }\n"
                + "}\n";
    }

    private String controller(String packageBase, String entity, String variable, String tableName) {
        return "package " + packageBase + ".controller;\n\n"
                + "import " + packageBase + ".dto." + entity + "Request;\n"
                + "import " + packageBase + ".dto." + entity + "Response;\n"
                + "import " + packageBase + ".service." + entity + "Service;\n"
                + "import jakarta.validation.Valid;\n"
                + "import java.util.List;\n"
                + "import org.springframework.http.HttpStatus;\n"
                + "import org.springframework.web.bind.annotation.*;\n\n"
                + "@RestController\n"
                + "@RequestMapping(\"/api/" + tableName + "\")\n"
                + "public class " + entity + "Controller {\n\n"
                + "    private final " + entity + "Service " + variable + "Service;\n\n"
                + "    public " + entity + "Controller(" + entity + "Service " + variable + "Service) {\n"
                + "        this." + variable + "Service = " + variable + "Service;\n"
                + "    }\n\n"
                + "    @PostMapping\n"
                + "    @ResponseStatus(HttpStatus.CREATED)\n"
                + "    public " + entity + "Response create(@Valid @RequestBody " + entity + "Request request) {\n"
                + "        return " + variable + "Service.create(request);\n"
                + "    }\n\n"
                + "    @GetMapping\n"
                + "    public List<" + entity + "Response> getAll() {\n"
                + "        return " + variable + "Service.getAll();\n"
                + "    }\n\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public " + entity + "Response getById(@PathVariable Long id) {\n"
                + "        return " + variable + "Service.getById(id);\n"
                + "    }\n\n"
                + "    @PutMapping(\"/{id}\")\n"
                + "    public " + entity + "Response update(@PathVariable Long id, @Valid @RequestBody " + entity + "Request request) {\n"
                + "        return " + variable + "Service.update(id, request);\n"
                + "    }\n\n"
                + "    @DeleteMapping(\"/{id}\")\n"
                + "    @ResponseStatus(HttpStatus.NO_CONTENT)\n"
                + "    public void delete(@PathVariable Long id) {\n"
                + "        " + variable + "Service.delete(id);\n"
                + "    }\n"
                + "}\n";
    }

    private String entityField(FieldDefinitionRequest field) {
        if ("id".equalsIgnoreCase(field.name())) {
            return """
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;""";
        }

        String prefix = field.required() ? "@Column(nullable = false)\n    " : "";
        return prefix + "private " + normalizeType(field.type()) + " " + field.name() + ";";
    }

    private String dtoField(FieldDefinitionRequest field) {
        return validationFor(field) + "private " + normalizeType(field.type()) + " " + field.name() + ";";
    }

    private String validationFor(FieldDefinitionRequest field) {
        if (!field.required()) {
            return "";
        }
        return "String".equals(normalizeType(field.type())) ? "@NotBlank\n    " : "@NotNull\n    ";
    }

    private String normalizeType(String type) {
        return switch (type.trim()) {
            case "string", "String" -> "String";
            case "long", "Long" -> "Long";
            case "int", "Integer", "integer" -> "Integer";
            case "double", "Double" -> "Double";
            case "bigdecimal", "BigDecimal" -> "BigDecimal";
            case "boolean", "Boolean" -> "Boolean";
            default -> type.trim();
        };
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String decapitalize(String value) {
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }
}
