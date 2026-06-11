package org.tkit.onecx.onecxbffgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ParentVersionResolver {

    private static final String FALLBACK_VERSION = "3.1.0";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/onecx/onecx-quarkus3-parent/releases/latest";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ParentVersionResolver() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
    }

    ParentVersionResolver(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String resolve(String inputVersion) {
        if (inputVersion != null && !inputVersion.isBlank()) {
            return normalize(inputVersion);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = objectMapper.readTree(response.body());
                String tag = node.path("tag_name").asText();
                if (!tag.isBlank()) {
                    return normalize(tag);
                }
            }
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return FALLBACK_VERSION;
    }

    private String normalize(String raw) {
        return raw.trim().replaceFirst("^[vV]", "");
    }
}



