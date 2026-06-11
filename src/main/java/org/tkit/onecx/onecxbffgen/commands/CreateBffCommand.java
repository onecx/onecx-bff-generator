package org.tkit.onecx.onecxbffgen.commands;

import org.tkit.onecx.onecxbffgen.model.GenerateRequest;
import org.tkit.onecx.onecxbffgen.service.GeneratorService;

import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "create-bff",
        mixinStandardHelpOptions = true,
        description = "Creates a OneCX BFF project from frontend/backend OpenAPI sources"
)
public class CreateBffCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "--name", "--project-name" }, required = true, description = "Display project name")
    String projectName;

    @CommandLine.Option(names = { "--group", "--group-id" }, defaultValue = "org.tkit.onecx", description = "Maven groupId")
    String groupId;

    @CommandLine.Option(names = "--package", description = "Base Java package (if missing, derived from group + artifact)")
    String packageName;

    @CommandLine.Option(names = "--artifact-id", description = "Maven artifactId (defaults to sanitized project name)")
    String artifactId;

    @CommandLine.Option(names = "--frontend-api", required = true, description = "Path or URL to frontend OpenAPI")
    String frontendApi;

    @CommandLine.Option(names = "--backend-api", required = true, description = "Path or URL to backend OpenAPI")
    String backendApi;

    @CommandLine.Option(names = "--output-dir", defaultValue = ".", description = "Output directory for generated project")
    Path outputDir;

    @CommandLine.Option(names = "--autobuild", defaultValue = "false",
            description = "Run mvn -B -ntp -DskipTests clean package in generated project")
    boolean autoBuild;

    @CommandLine.Option(names = "--parent-version", description = "onecx-quarkus3-parent version; if missing, latest release is used")
    String parentVersion;

    private final GeneratorService generatorService;

    public CreateBffCommand(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @Override
    public Integer call() {
        GenerateRequest request = new GenerateRequest(
                projectName,
                groupId,
                packageName,
                artifactId,
                frontendApi,
                backendApi,
                outputDir,
                autoBuild,
                parentVersion
        );

        try {
            Path result = generatorService.generate(request);
            System.out.println("Generated BFF project in: " + result.toAbsolutePath());
            return 0;
        } catch (Exception e) {
            System.err.println("Generation failed: " + e.getMessage());
            return 1;
        }
    }
}






