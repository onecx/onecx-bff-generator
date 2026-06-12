package org.tkit.onecx.onecxbffgen.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TemplateService {

    public void renderToFile(String templateName, Path outputPath, Map<String, ?> values) {
        try {
            String rendered = render(templateName, values);

            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(outputPath, rendered, StandardCharsets.UTF_8);

            System.out.println("Rendering template: " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Template render failed: " + templateName, e);
        }
    }

    public String render(String templateName, Map<String, ?> values) {
        String template = readTemplate(templateName);
        String result = template;

        if (values != null) {
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());

                result = result.replace("${" + key + "}", value);
                result = result.replace("${" + key + "!}", value);
            }
        }

        return result;
    }

    public boolean exists(String templateName) {
        return resolveTemplatePath(templateName) != null;
    }

    private String readTemplate(String templateName) {
        String resolvedPath = resolveTemplatePath(templateName);

        if (resolvedPath == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }

        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resolvedPath)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Template not found: " + resolvedPath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IllegalStateException("Could not read template: " + resolvedPath, e);
        }
    }

    private String resolveTemplatePath(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return null;
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String candidate : candidates(templateName)) {
            if (classLoader.getResource(candidate) != null) {
                return candidate;
            }
        }

        return null;
    }

    private Set<String> candidates(String templateName) {
        String normalized = templateName.replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        Set<String> result = new LinkedHashSet<>();

        result.add(normalized);

        if (!normalized.startsWith("templates/")) {
            result.add("templates/" + normalized);
        }

        if (normalized.startsWith("templates/")) {
            result.add(normalized.substring("templates/".length()));
        }

        return result;
    }
}