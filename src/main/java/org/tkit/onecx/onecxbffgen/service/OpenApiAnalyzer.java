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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        String requestBodyType = extractRequestBodyType(operation);
        String responseType = extractResponseType(operation);
        int successStatusCode = extractSuccessStatusCode(operation);
        controllers.computeIfAbsent(tag, ignored -> new ArrayList<>())
                .add(new OperationModel(operationId, normalizedMethod, path, requestBodyType, responseType, successStatusCode));
    }

    private String extractRequestBodyType(Operation operation) {
        if (operation.getRequestBody() == null) {
            return null;
        }
        var content = operation.getRequestBody().getContent();
        if (content == null) {
            return null;
        }
        var mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = content.values().stream().findFirst().orElse(null);
        }
        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }
        return schemaToSimpleType(mediaType.getSchema());
    }

    private String extractResponseType(Operation operation) {
        if (operation.getResponses() == null) {
            return null;
        }
        var successResponse = operation.getResponses().get("200");
        if (successResponse == null) {
            successResponse = operation.getResponses().get("201");
        }
        if (successResponse == null) {
            return null;
        }
        var content = successResponse.getContent();
        if (content == null) {
            return null;
        }
        var mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = content.values().stream().findFirst().orElse(null);
        }
        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }
        return schemaToSimpleType(mediaType.getSchema());
    }

    private int extractSuccessStatusCode(Operation operation) {
        if (operation.getResponses() == null) {
            return 0;
        }
        for (String code : List.of("200", "201", "202", "204")) {
            if (operation.getResponses().containsKey(code)) {
                return Integer.parseInt(code);
            }
        }
        return 0;
    }

    @SuppressWarnings("rawtypes")
    private String schemaToSimpleType(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
        }
        if ("array".equals(schema.getType())) {
            Schema items = schema.getItems();
            if (items != null && items.get$ref() != null) {
                return "List<" + items.get$ref().substring(items.get$ref().lastIndexOf('/') + 1) + ">";
            }
            return "List<Object>";
        }
        return null;
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

    public Set<String> extractPermissionKeys(OpenAPI api) {
        Set<String> keys = new LinkedHashSet<>();
        if (api.getPaths() == null) return keys;
        api.getPaths().forEach((path, pathItem) -> {
            if (pathItem == null) return;
            for (Operation op : pathItem.readOperations()) {
                if (op == null || op.getExtensions() == null) continue;
                Object xOnecx = op.getExtensions().get("x-onecx");
                if (xOnecx instanceof Map<?, ?> xOnecxMap) {
                    Object permissions = xOnecxMap.get("permissions");
                    if (permissions instanceof Map<?, ?> permMap) {
                        permMap.keySet().forEach(k -> keys.add(String.valueOf(k)));
                    }
                }
            }
        });
        return keys;
    }
}
