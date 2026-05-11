package org.tkit.onecx.onecxbffgen.service;

import org.tkit.onecx.onecxbffgen.model.OperationModel;
import org.tkit.onecx.onecxbffgen.model.SchemaModel;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenApiAnalyzer {

    public OpenAPI read(Path path) {
        OpenAPI api = new OpenAPIV3Parser().read(path.toString());
        if (api == null) {
            throw new IllegalArgumentException("Could not parse OpenAPI file: " + path);
        }
        return api;
    }

    public List<SchemaModel> extractSchemas(OpenAPI api) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
            return List.of();
        }

        List<SchemaModel> result = new ArrayList<>();
        api.getComponents().getSchemas().forEach((name, schema) -> {
            String schemaName = String.valueOf(name);
            Map<String, String> fields = new LinkedHashMap<>();
            if (schema.getProperties() != null) {
                schema.getProperties().forEach((fieldName, fieldSchema) -> {
                    Schema<?> resolved = (Schema<?>) fieldSchema;
                    fields.put(String.valueOf(fieldName), toJavaType(resolved));
                });
            }
            result.add(new SchemaModel(schemaName, fields));
        });
        return result;
    }

    public Map<String, List<OperationModel>> extractControllers(OpenAPI api) {
        if (api.getPaths() == null) {
            return Map.of();
        }

        Map<String, List<OperationModel>> controllers = new LinkedHashMap<>();
        api.getPaths().forEach((path, pathItem) -> {
            addOperation(controllers, "get", path, pathItem.getGet());
            addOperation(controllers, "post", path, pathItem.getPost());
            addOperation(controllers, "put", path, pathItem.getPut());
            addOperation(controllers, "delete", path, pathItem.getDelete());
            addOperation(controllers, "patch", path, pathItem.getPatch());
        });
        return controllers;
    }

    private void addOperation(Map<String, List<OperationModel>> controllers,
                              String method,
                              String path,
                              Operation operation) {
        if (operation == null) {
            return;
        }
        String operationId = operation.getOperationId();
        if (operationId == null || operationId.isBlank()) {
            return;
        }
        String tag = resolveControllerTag(operation.getTags(), path);
        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        controllers.computeIfAbsent(tag, ignored -> new ArrayList<>())
                .add(new OperationModel(operationId, normalizedMethod, path));
    }

    private String resolveControllerTag(List<String> tags, String path) {
        if (tags != null && !tags.isEmpty()) {
            String tag = tags.get(0);
            if (tag != null && !tag.isBlank()) {
                return sanitizeTag(tag);
            }
        }
        return inferControllerTag(path);
    }

    private String sanitizeTag(String tag) {
        String[] parts = tag.replaceAll("[^a-zA-Z0-9]", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? "Default" : sb.toString();
    }

    private String inferControllerTag(String path) {
        String clean = path.startsWith("/") ? path.substring(1) : path;
        if (clean.isBlank()) {
            return "Default";
        }
        String firstSegment = clean.split("/")[0];
        if (firstSegment.startsWith("{")) {
            return "Default";
        }
        return Character.toUpperCase(firstSegment.charAt(0)) + firstSegment.substring(1);
    }

    private String toJavaType(Schema<?> schema) {
        if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
            return schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
        }
        String type = schema.getType();
        String format = schema.getFormat();

        if ("integer".equals(type)) {
            return "int64".equals(format) ? "Long" : "Integer";
        }
        if ("number".equals(type)) {
            return "float".equals(format) ? "Float" : "Double";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if ("array".equals(type)) {
            return "java.util.List<Object>";
        }
        return "String";
    }
}


