package org.tkit.onecx.onecxbffgen.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ApiSourceResolver {

    private final HttpClient httpClient;

    public ApiSourceResolver() {
        this(HttpClient.newHttpClient());
    }

    ApiSourceResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Path copyTo(String source, Path target) throws IOException, InterruptedException {
        Files.createDirectories(target.getParent());
        if (isHttp(source)) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(source)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Could not fetch URL: " + source + " (status=" + response.statusCode() + ")");
            }
            Files.write(target, response.body());
            return target;
        }

        Path localPath = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
        if (!Files.exists(localPath)) {
            throw new IOException("OpenAPI source does not exist: " + source);
        }
        Files.copy(localPath, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private boolean isHttp(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }
}




