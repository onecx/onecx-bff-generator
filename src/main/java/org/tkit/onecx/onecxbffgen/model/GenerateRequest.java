package org.tkit.onecx.onecxbffgen.model;

import java.nio.file.Path;

public record GenerateRequest(
        String projectName,
        String groupId,
        String packageName,
        String artifactId,
        String frontendApi,
        String backendApi,
        Path outputDir,
        boolean autoBuild,
        String parentVersion
) {
}





