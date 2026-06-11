package org.tkit.onecx.onecxbffgen.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TemplateRenderer {

    public String render(String templateName, Map<String, String> values) {
        String template = readTemplate(templateName);
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String readTemplate(String templateName) {
        String path = "templates/" + templateName;
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Template not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read template: " + path, e);
        }
    }
}


