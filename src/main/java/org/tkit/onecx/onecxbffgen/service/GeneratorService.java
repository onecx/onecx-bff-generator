package org.tkit.onecx.onecxbffgen.service;
import io.swagger.v3.oas.models.OpenAPI;
import org.tkit.onecx.onecxbffgen.model.DependencyProfile;
import org.tkit.onecx.onecxbffgen.model.GenerateRequest;
import org.tkit.onecx.onecxbffgen.model.OperationModel;
import org.tkit.onecx.onecxbffgen.model.SchemaModel;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public class GeneratorService {
    private final ParentVersionResolver parentVersionResolver;
    private final ApiSourceResolver apiSourceResolver;
    private final OpenApiAnalyzer openApiAnalyzer;
    private final ProjectWriter projectWriter;
    public GeneratorService() {
        this(new ParentVersionResolver(), new ApiSourceResolver(), new OpenApiAnalyzer(), new ProjectWriter());
    }
    GeneratorService(ParentVersionResolver parentVersionResolver,
                     ApiSourceResolver apiSourceResolver,
                     OpenApiAnalyzer openApiAnalyzer,
                     ProjectWriter projectWriter) {
        this.parentVersionResolver = parentVersionResolver;
        this.apiSourceResolver = apiSourceResolver;
        this.openApiAnalyzer = openApiAnalyzer;
        this.projectWriter = projectWriter;
    }
    public Path generate(GenerateRequest request) throws IOException, InterruptedException {
        String artifactId = sanitizeArtifactId(request);
        String parentVersion = parentVersionResolver.resolve(request.parentVersion());
        DependencyProfile profile = DependencyProfile.fromParentVersion(parentVersion);
        String basePackage = resolveBasePackage(request, artifactId);
        Path projectDir = resolveProjectDir(request.outputDir(), artifactId);
        Files.createDirectories(projectDir);
        Path frontendFile = apiSourceResolver.copyTo(request.frontendApi(),
                resolveOpenApiTargetPath(request.frontendApi(), projectDir, "frontend"));
        Path backendFile = apiSourceResolver.copyTo(request.backendApi(),
                resolveOpenApiTargetPath(request.backendApi(), projectDir, "backend"));
        OpenAPI frontendApi = openApiAnalyzer.read(frontendFile);
        OpenAPI backendApi = openApiAnalyzer.read(backendFile);
        List<SchemaModel> frontendSchemas = openApiAnalyzer.extractSchemas(frontendApi);
        List<SchemaModel> backendSchemas = openApiAnalyzer.extractSchemas(backendApi);
        Map<String, List<OperationModel>> frontendControllers = openApiAnalyzer.extractControllers(frontendApi);
        Map<String, List<OperationModel>> backendControllers = openApiAnalyzer.extractControllers(backendApi);
        ControllerSelection controllerSelection = selectControllers(frontendControllers, backendControllers, frontendSchemas);
        projectWriter.writePom(projectDir, request.projectName(), request.groupId(), artifactId, parentVersion, profile,
                basePackage, frontendFile.getFileName().toString());
        projectWriter.writeApplicationFiles(projectDir, request.projectName(), request.groupId(), basePackage,
                artifactId,
                backendFile.getFileName().toString());
        projectWriter.writeGeneratedReadme(projectDir, request.projectName(), request.groupId(), basePackage, parentVersion,
                profile);
        projectWriter.writeControllerClasses(projectDir,
                basePackage,
                controllerSelection.controllers(),
                controllerSelection.backendClientByController(),
                controllerSelection.implementFrontendApi(),
                controllerSelection.todoStubMode());
        projectWriter.writeMapperClasses(projectDir, basePackage, frontendSchemas, backendSchemas);
        projectWriter.writeTestScaffold(projectDir, basePackage, controllerSelection.controllers());
        projectWriter.writeWorkflowFiles(projectDir, profile);
        writeGenerationReport(projectDir, request.projectName(), request.groupId(), basePackage, parentVersion, profile,
                frontendSchemas, backendSchemas, controllerSelection.controllers());
        boolean latestResolved = request.parentVersion() == null || request.parentVersion().isBlank();
        System.out.println("Generated BFF '" + request.projectName() + "' in: " + projectDir.toAbsolutePath());
        System.out.println("Resolved onecx-quarkus3-parent version: " + parentVersion
                + (latestResolved ? " (latest release)" : " (from --parent-version)"));
        if (request.autoBuild()) {
            runAutoBuild(projectDir);
        }
        return projectDir;
    }

    private void runAutoBuild(Path projectDir) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("mvn", "-B", "-ntp", "-DskipTests", "clean", "package");
        processBuilder.directory(projectDir.toFile());
        processBuilder.inheritIO();
        int exitCode = processBuilder.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Autobuild failed with exit code " + exitCode);
        }
    }
    private String sanitizeArtifactId(GenerateRequest request) {
        String raw = request.artifactId() == null || request.artifactId().isBlank()
                ? request.projectName()
                : request.artifactId();
        String clean = raw.toLowerCase().replaceAll("[^a-z0-9.-]", "-");
        String normalized = clean.replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Could not derive artifactId from input");
        }
        return normalized;
    }
    private String deriveBasePackage(String groupId, String artifactId) {
        List<String> groupTokens = new ArrayList<>(List.of(groupId.split("\\.")));
        List<String> artifactTokens = new ArrayList<>(List.of(artifactId.split("[-.]")));
        groupTokens.removeIf(String::isBlank);
        artifactTokens.removeIf(String::isBlank);
        if (!groupTokens.isEmpty() && !artifactTokens.isEmpty()) {
            String groupTail = groupTokens.get(groupTokens.size() - 1);
            if (groupTail.equals(artifactTokens.get(0))) {
                artifactTokens.remove(0);
            }
        }
        if (artifactTokens.isEmpty()) {
            return String.join(".", groupTokens);
        }
        return String.join(".", groupTokens) + "." + String.join(".", artifactTokens);
    }
    private String resolveBasePackage(GenerateRequest request, String artifactId) {
        String candidate;
        if (request.packageName() != null && !request.packageName().isBlank()) {
            candidate = request.packageName().trim();
        } else {
            candidate = deriveBasePackage(request.groupId(), artifactId);
        }
        return ensureBffPackage(candidate, artifactId);
    }
    private String ensureBffPackage(String packageName, String artifactId) {
        if (!(artifactId.equalsIgnoreCase("bff")
                || artifactId.toLowerCase().endsWith("-bff")
                || artifactId.toLowerCase().contains(".bff"))) {
            return packageName;
        }
        List<String> tokens = List.of(packageName.split("\\."));
        if (tokens.stream().anyMatch("bff"::equalsIgnoreCase)) {
            return packageName;
        }
        return packageName + ".bff";
    }
    private Path resolveProjectDir(Path outputDir, String artifactId) {
        if (outputDir == null) {
            return Path.of(artifactId);
        }
        Path normalized = outputDir.normalize();
        Path fileName = normalized.getFileName();
        if (fileName != null && artifactId.equals(fileName.toString())) {
            return normalized;
        }
        return normalized.resolve(artifactId);
    }
    private Path resolveOpenApiTargetPath(String source, Path projectDir, String side) {
        return projectDir.resolve("src/main/openapi/" + side + "/" + resolveSourceFileName(source, side));
    }
    private String resolveSourceFileName(String source, String side) {
        try {
            Path fileName;
            if (source.startsWith("http://") || source.startsWith("https://")) {
                String path = URI.create(source).getPath();
                fileName = path == null || path.isBlank() ? null : Path.of(path).getFileName();
            } else if (source.startsWith("file:")) {
                fileName = Path.of(URI.create(source)).getFileName();
            } else {
                fileName = Path.of(source).getFileName();
            }
            if (fileName != null && !fileName.toString().isBlank()) {
                return fileName.toString();
            }
        } catch (Exception ignored) {
            // fallback below keeps generation robust even for unusual sources
        }
        return side + "-openapi.yaml";
    }

    private ControllerSelection selectControllers(Map<String, List<OperationModel>> frontendControllers,
                                                  Map<String, List<OperationModel>> backendControllers,
                                                  List<SchemaModel> frontendSchemas) {
        if (!frontendControllers.isEmpty()) {
            Map<String, String> backendClientByController = new LinkedHashMap<>();
            frontendControllers.keySet().forEach(key -> backendClientByController.put(key, key));
            return new ControllerSelection(frontendControllers, backendClientByController, true, false);
        }
        if (backendControllers.isEmpty()) {
            return new ControllerSelection(Map.of(), Map.of(), false, true);
        }

        String primaryEntity = resolvePrimaryEntity(frontendSchemas);
        String pathToken = "/" + primaryEntity.toLowerCase() + "s";
        List<OperationModel> selected = new ArrayList<>();
        String backendClientBase = null;
        for (Map.Entry<String, List<OperationModel>> entry : backendControllers.entrySet()) {
            List<OperationModel> matching = entry.getValue().stream()
                    .filter(op -> op.path() != null && op.path().toLowerCase().contains(pathToken))
                    .toList();
            if (!matching.isEmpty()) {
                selected.addAll(matching);
                if (backendClientBase == null) {
                    backendClientBase = entry.getKey();
                }
            }
        }
        if (selected.isEmpty()) {
            Map.Entry<String, List<OperationModel>> first = backendControllers.entrySet().iterator().next();
            selected = first.getValue();
            backendClientBase = first.getKey();
        }

        Map<String, List<OperationModel>> controllers = Map.of(primaryEntity, selected);
        Map<String, String> backendClientByController = Map.of(primaryEntity, backendClientBase);
        return new ControllerSelection(controllers, backendClientByController, false, true);
    }

    private String resolvePrimaryEntity(List<SchemaModel> schemas) {
        SchemaModel best = null;
        int bestScore = Integer.MIN_VALUE;
        for (SchemaModel schema : schemas) {
            String name = schema.name();
            String normalized = name == null ? "" : name;
            if (normalized.startsWith("ProblemDetail")
                    || normalized.equalsIgnoreCase("OffsetDateTime")
                    || normalized.endsWith("SearchCriteria")
                    || normalized.endsWith("PageResult")) {
                continue;
            }
            int refScore = (int) schema.fields().values().stream().filter(v -> v != null && Character.isUpperCase(v.charAt(0))).count();
            int score = schema.fields().size() + (refScore * 2);
            if (best == null || score > bestScore) {
                best = schema;
                bestScore = score;
            }
        }
        if (best != null) {
            return best.name();
        }
        return "Default";
    }

    private record ControllerSelection(Map<String, List<OperationModel>> controllers,
                                       Map<String, String> backendClientByController,
                                       boolean implementFrontendApi,
                                       boolean todoStubMode) {
    }

    private void writeGenerationReport(Path projectDir,
                                       String projectName,
                                       String groupId,
                                       String basePackage,
                                       String parentVersion,
                                       DependencyProfile profile,
                                       List<SchemaModel> frontendSchemas,
                                       List<SchemaModel> backendSchemas,
                                       Map<String, List<OperationModel>> controllers) throws IOException {
        String report = """
                {
                  "projectName": "%s",
                  "groupId": "%s",
                  "basePackage": "%s",
                  "parentVersion": "%s",
                  "dependencyProfile": "%s",
                  "frontendSchemas": %d,
                  "backendSchemas": %d,
                  "controllers": %d
                }
                """.formatted(projectName, groupId, basePackage, parentVersion, profile.name(), frontendSchemas.size(),
                backendSchemas.size(), controllers.size());
        Files.writeString(projectDir.resolve("generation-report.json"), report);
    }
}



