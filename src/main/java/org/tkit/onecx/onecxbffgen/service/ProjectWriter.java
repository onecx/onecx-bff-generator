package org.tkit.onecx.onecxbffgen.service;
import org.tkit.onecx.onecxbffgen.model.DependencyProfile;
import org.tkit.onecx.onecxbffgen.model.OperationModel;
import org.tkit.onecx.onecxbffgen.model.SchemaModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class ProjectWriter {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}/]+)}");
    private static final String BACKEND_CONFIG_KEY = "backend_api";
    private final TemplateRenderer templateRenderer;
    public ProjectWriter() {
        this(new TemplateRenderer());
    }
    ProjectWriter(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }
    public void writePom(Path projectDir,
                         String projectName,
                         String groupId,
                         String artifactId,
                         String parentVersion,
                         DependencyProfile profile,
                         String basePackage,
                         String frontendFileName) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("projectName", projectName);
        values.put("parentVersion", parentVersion);
        values.put("groupId", groupId);
        values.put("artifactId", artifactId);
        values.put("packaging", profile == DependencyProfile.MODERN_3_1_PLUS ? "    <packaging>quarkus</packaging>" : "");
        values.put("javaVersion", javaVersion(profile));
        values.put("junitArtifact", usesLegacyJunitArtifacts(profile) ? "quarkus-junit5" : "quarkus-junit");
        values.put("junitMockitoArtifact",
                usesLegacyJunitArtifacts(profile) ? "quarkus-junit5-mockito" : "quarkus-junit-mockito");
        values.put("mockserverSwaggerParserExclusion", profile == DependencyProfile.LEGACY_UP_TO_2_5
                ? "            <exclusions>\n                <exclusion>\n                    <groupId>io.swagger.parser.v3</groupId>\n                    <artifactId>swagger-parser</artifactId>\n                </exclusion>\n            </exclusions>\n"
                : "");
        values.put("legacySwaggerParser", profile == DependencyProfile.LEGACY_UP_TO_2_5
                ? "        <dependency>\n            <groupId>io.swagger.parser.v3</groupId>\n            <artifactId>swagger-parser</artifactId>\n            <scope>test</scope>\n        </dependency>\n"
                : "");
        values.put("openApiJavaOption", profile == DependencyProfile.MODERN_3_1_PLUS ? "" : "                        <java17>true</java17>");
        values.put("frontendApiFileName", frontendFileName);
        values.put("internalApiPackage", "gen." + basePackage + ".rs.internal");
        values.put("internalModelPackage", "gen." + basePackage + ".rs.internal.model");
        writeTemplate(projectDir.resolve("pom.xml"), "bff-project/pom.xml.tpl", values);
    }
    public void writeGeneratedReadme(Path projectDir,
                                     String projectName,
                                     String groupId,
                                     String basePackage,
                                     String parentVersion,
                                     DependencyProfile profile) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("projectName", projectName);
        values.put("groupId", groupId);
        values.put("basePackage", basePackage);
        values.put("parentVersion", parentVersion);
        values.put("dependencyProfile", profile.name());
        values.put("javaVersion", javaVersion(profile));
        writeTemplate(projectDir.resolve("README.md"), "bff-project/README.md.tpl", values);
    }
    public void writeApplicationFiles(Path projectDir,
                                      String projectName,
                                      String groupId,
                                      String basePackage,
                                      String artifactId,
                                      String backendFileName) throws IOException {
        Map<String, String> appValues = new LinkedHashMap<>();
        appValues.put("projectName", projectName);
        appValues.put("groupId", groupId);
        appValues.put("basePackage", basePackage);
        appValues.put("backendSpecKey", toPropertyToken(backendFileName));
        appValues.put("backendClientBasePackage", "gen." + basePackage + ".backend.client");
        appValues.put("backendConfigKey", BACKEND_CONFIG_KEY);
        writeTemplate(projectDir.resolve("src/main/resources/application.properties"),
                "bff-project/application.properties.tpl", appValues);
        writeTemplate(projectDir.resolve(".gitignore"), "bff-project/gitignore.tpl", Map.of());
        writeTemplate(projectDir.resolve("src/main/helm/Chart.yaml"), "bff-project/Chart.yaml.tpl",
                Map.of("artifactId", artifactId, "projectName", projectName));
        writeTemplate(projectDir.resolve("src/main/helm/values.yaml"), "bff-project/values.yaml.tpl",
                Map.of("artifactId", artifactId, "projectName", projectName));
        writeTemplate(projectDir.resolve(artifactId + ".adoc"), "bff-project/project.adoc.tpl",
                Map.of("projectName", projectName));
        writeTemplate(projectDir.resolve("src/main/docker/Dockerfile.jvm"), "bff-project/Dockerfile.jvm.tpl", Map.of());
        writeTemplate(projectDir.resolve("src/main/docker/Dockerfile.native"), "bff-project/Dockerfile.native.tpl", Map.of());
    }
    public void writeWorkflowFiles(Path projectDir, DependencyProfile profile) throws IOException {
        for (WorkflowDefinition workflow : WorkflowDefinition.standard(javaVersion(profile))) {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("workflowName", workflow.workflowName());
            values.put("triggerBlock", workflow.triggerBlock());
            values.put("jobName", workflow.jobName());
            values.put("runCommand", workflow.runCommand());
            values.put("javaVersion", workflow.javaVersion());
            writeTemplate(projectDir.resolve(".github/workflows/" + workflow.fileName()),
                    "bff-project/workflow.yml.tpl", values);
        }
    }
    public void writeControllerClasses(Path projectDir,
                                       String pkg,
                                       Map<String, List<OperationModel>> controllers,
                                       Map<String, String> backendClientByController,
                                       boolean implementFrontendApi,
                                       boolean todoStubMode) throws IOException {
        Path baseDir = projectDir.resolve("src/main/java/" + pkg.replace('.', '/') + "/rs/controllers");
        for (Map.Entry<String, List<OperationModel>> entry : new TreeMap<>(controllers).entrySet()) {
            String controllerBaseName = sanitizeTypeName(entry.getKey());
            String controllerName = controllerBaseName + "RestController";
            String backendClientBase = sanitizeTypeName(backendClientByController.getOrDefault(entry.getKey(), entry.getKey()));
            String mapperType = mapperBaseName(controllerBaseName) + "Mapper";
            Map<String, String> values = new LinkedHashMap<>();
            values.put("packageName", pkg + ".rs.controllers");
            values.put("className", controllerName);
            values.put("exceptionMapperImport", pkg + ".rs.mappers.ExceptionMapper");
            values.put("apiServiceImportStatement", implementFrontendApi
                    ? "import gen." + pkg + ".rs.internal." + controllerBaseName + "ApiService;"
                    : "");
            values.put("apiServiceTypeSuffix", implementFrontendApi ? " implements " + controllerBaseName + "ApiService" : "");
            values.put("backendClientImport", "gen." + pkg + ".backend.client.api." + backendClientBase + "Api");
            values.put("backendClientType", backendClientBase + "Api");
            values.put("mapperImport", pkg + ".rs.mappers." + mapperType);
            values.put("mapperType", mapperType);
            values.put("methods", buildControllerMethods(entry.getValue(), implementFrontendApi, todoStubMode));
            writeTemplate(baseDir.resolve(controllerName + ".java"), "entity/Controller.java.tpl", values);
        }
    }
    public void writeMapperClasses(Path projectDir, String pkg, List<SchemaModel> frontend, List<SchemaModel> backend) throws IOException {
        Path baseDir = projectDir.resolve("src/main/java/" + pkg.replace('.', '/') + "/rs/mappers");
        Map<String, SchemaModel> backendByNormalized = new TreeMap<>();
        for (SchemaModel schema : backend) {
            backendByNormalized.put(normalizeEntityName(schema.name()), schema);
        }
        Map<String, String> normalizedToMapper = new LinkedHashMap<>();
        for (SchemaModel source : frontend) {
            String normalized = normalizeEntityName(source.name());
            SchemaModel target = backendByNormalized.get(normalized);
            if (target == null || !shouldGenerateMapper(source.name())) {
                continue;
            }
            normalizedToMapper.put(normalized, mapperBaseName(sanitizeTypeName(source.name())) + "Mapper");
        }
        writeTemplate(baseDir.resolve("ExceptionMapper.java"), "entity/ExceptionMapper.java.tpl",
                Map.of("packageName", pkg + ".rs.mappers"));
        for (SchemaModel source : frontend) {
            String normalized = normalizeEntityName(source.name());
            SchemaModel target = backendByNormalized.get(normalized);
            if (target == null || !shouldGenerateMapper(source.name())) {
                continue;
            }
            String sourceType = sanitizeTypeName(source.name());
            String targetType = sanitizeTypeName(target.name());
            String mapperName = mapperBaseName(sourceType) + "Mapper";
            String sourceModelType = sourceType + "DTO";
            String targetModelType = targetType;
            String sourceImport = "gen." + pkg + ".rs.internal.model." + sourceModelType;
            String targetImport = "gen." + pkg + ".backend.client.model." + targetModelType;
            boolean sameSimpleType = sourceModelType.equals(targetModelType);
            Map<String, String> values = new LinkedHashMap<>();
            values.put("packageName", pkg + ".rs.mappers");
            values.put("sourceImportStatement", sameSimpleType ? "" : "import " + sourceImport + ";");
            values.put("targetImportStatement", sameSimpleType ? "" : "import " + targetImport + ";");
            values.put("className", mapperName);
            values.put("sourceTypeRef", sameSimpleType ? sourceImport : sourceModelType);
            values.put("targetTypeRef", sameSimpleType ? targetImport : targetModelType);
            values.put("usesClause", buildMapperUsesClause(source, normalizedToMapper));
            writeTemplate(baseDir.resolve(mapperName + ".java"), "entity/Mapper.java.tpl", values);
        }
    }
    public void writeTestScaffold(Path projectDir, String pkg, Map<String, List<OperationModel>> controllers) throws IOException {
        Path baseDir = projectDir.resolve("src/test/java/" + pkg.replace('.', '/') + "/rs");
        writeTemplate(baseDir.resolve("AbstractTest.java"), "test/AbstractTest.java.tpl",
                Map.of("packageName", pkg + ".rs"));
        for (String key : new TreeMap<>(controllers).keySet()) {
            String controllerName = sanitizeTypeName(key) + "RestController";
            Map<String, String> values = new LinkedHashMap<>();
            values.put("packageName", pkg + ".rs");
            values.put("controllerImport", pkg + ".rs.controllers." + controllerName);
            values.put("className", controllerName);
            values.put("endpointTests", buildEndpointTests(controllers.getOrDefault(key, List.of())));
            writeTemplate(baseDir.resolve(controllerName + "Test.java"), "test/ControllerTest.java.tpl", values);
            writeTemplate(baseDir.resolve(controllerName + "IT.java"), "test/ControllerIT.java.tpl", values);
        }
        Path resourcesDir = projectDir.resolve("src/test/resources");
        writeTemplate(resourcesDir.resolve("mockserver.properties"), "test/mockserver.properties.tpl", Map.of());
        writeTemplate(resourcesDir.resolve("mockserver/mandatory_tokens.json"), "test/mockserver-mandatory-tokens.json.tpl", Map.of());
        writeTemplate(resourcesDir.resolve("mockserver/permissions.json"), "test/mockserver-permissions.json.tpl", Map.of());
    }
    private String buildControllerMethods(List<OperationModel> operations, boolean implementFrontendApi, boolean todoStubMode) {
        StringBuilder sb = new StringBuilder();
        for (OperationModel op : operations) {
            List<String> params = pathParams(op.path());
            String signature = methodSignature(params);
            sb.append("    @").append(op.httpMethod()).append("\n")
                    .append("    @Path(\"").append(op.path()).append("\")\n")
                    .append(implementFrontendApi ? "    @Override\n" : "")
                    .append("    public Response ").append(sanitizeMethodName(op.operationId())).append("(")
                    .append(signature).append(") {\n");
            if (todoStubMode) {
                sb.append("        // TODO implement backend call and mapper conversion.\n")
                        .append("        return null;\n");
            } else {
                sb.append("        return Response.status(Response.Status.NOT_IMPLEMENTED)\n")
                        .append("                .entity(\"TODO: implement against generated backend client and mapper.\")\n")
                        .append("                .build();\n");
            }
            sb.append("    }\n\n");
        }
        return sb.toString();
    }

    private String buildEndpointTests(List<OperationModel> operations) {
        StringBuilder sb = new StringBuilder();
        for (OperationModel op : operations) {
            String methodName = sanitizeMethodName(op.operationId());
            String testName = "shouldHandle" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
            sb.append("    @Test\n")
                    .append("    void ").append(testName).append("() {\n")
                    .append("        // TODO add endpoint behavior assertions when frontend contract is finalized.\n")
                    .append("        assertNotNull(controller);\n")
                    .append("    }\n\n");
        }
        return sb.toString();
    }
    private List<String> pathParams(String path) {
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
    private String methodSignature(List<String> params) {
        return String.join(", ", params.stream()
                .map(name -> "@PathParam(\"" + name + "\") String " + sanitizeFieldName(name))
                .toList());
    }
    private void writeTemplate(Path path, String templateName, Map<String, String> values) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, templateRenderer.render(templateName, values));
    }
    private String javaVersion(DependencyProfile profile) {
        return profile == DependencyProfile.MODERN_3_1_PLUS ? "25" : "17";
    }
    private boolean usesLegacyJunitArtifacts(DependencyProfile profile) {
        return profile == DependencyProfile.LEGACY_UP_TO_2_5;
    }
    private String sanitizeTypeName(String value) {
        String clean = value.replaceAll("[^a-zA-Z0-9]", " ");
        String[] parts = clean.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? "GeneratedType" : sb.toString();
    }
    private String sanitizeFieldName(String value) {
        String cleaned = value.replaceAll("[^a-zA-Z0-9]", " ").trim();
        if (cleaned.isBlank()) {
            return "field";
        }
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, "field");
        }
        return sb.toString();
    }
    private String sanitizeMethodName(String value) {
        String name = sanitizeFieldName(value);
        if (Character.isDigit(name.charAt(0))) {
            return "op" + name;
        }
        return name;
    }
    private String normalizeEntityName(String value) {
        return sanitizeTypeName(value)
                .replaceAll("(Dto|DTO|Response|Request|Internal|External)$", "")
                .toLowerCase(Locale.ROOT);
    }
    private String mapperBaseName(String sanitizedTypeName) {
        String stripped = sanitizedTypeName.replaceAll("(Dto|DTO|Response|Request|Internal|External)$", "");
        return stripped.isBlank() ? sanitizedTypeName : stripped;
    }
    private boolean shouldGenerateMapper(String schemaName) {
        String sanitized = sanitizeTypeName(schemaName);
        return !(sanitized.startsWith("ProblemDetail")
                || sanitized.equals("OffsetDateTime")
                || sanitized.endsWith("SearchCriteria")
                || sanitized.endsWith("PageResult"));
    }
    private String buildMapperUsesClause(SchemaModel source, Map<String, String> normalizedToMapper) {
        List<String> uses = new ArrayList<>();
        String current = mapperBaseName(sanitizeTypeName(source.name())) + "Mapper";
        for (String type : source.fields().values()) {
            String mapper = normalizedToMapper.get(normalizeEntityName(type));
            if (mapper != null && !mapper.equals(current) && !uses.contains(mapper)) {
                uses.add(mapper);
            }
        }
        if (uses.isEmpty()) {
            return "";
        }
        return ", uses = { " + String.join(", ", uses.stream().map(use -> use + ".class").toList()) + " }";
    }
    private String toPropertyToken(String value) {
        String normalized = value.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return normalized.toLowerCase(Locale.ROOT);
    }
    private record WorkflowDefinition(String fileName,
                                      String workflowName,
                                      String triggerBlock,
                                      String jobName,
                                      String runCommand,
                                      String javaVersion) {
        private static List<WorkflowDefinition> standard(String javaVersion) {
            return List.of(
                    new WorkflowDefinition("build.yml", "build", "on:\n  push:\n    branches:\n      - main\n  workflow_dispatch:", "build", "mvn -B -ntp clean verify", javaVersion),
                    new WorkflowDefinition("build-pr.yml", "build-pr", "on:\n  pull_request:\n    branches:\n      - main", "build_pr", "mvn -B -ntp clean verify", javaVersion),
                    new WorkflowDefinition("build-pr-merge.yml", "build-pr-merge", "on:\n  pull_request:\n    types: [closed]\n    branches:\n      - main", "build_pr_merge", "mvn -B -ntp -DskipTests package", javaVersion),
                    new WorkflowDefinition("build-branch.yml", "build-branch", "on:\n  push:\n    branches-ignore:\n      - main", "build_branch", "mvn -B -ntp clean verify", javaVersion),
                    new WorkflowDefinition("build-release.yml", "build-release", "on:\n  workflow_dispatch:", "build_release", "mvn -B -ntp -DskipTests clean package", javaVersion),
                    new WorkflowDefinition("create-fix-branch.yml", "create-fix-branch", "on:\n  workflow_dispatch:", "create_fix_branch", "echo 'Create fix branch placeholder'", javaVersion),
                    new WorkflowDefinition("create-new-build.yml", "create-new-build", "on:\n  workflow_dispatch:", "create_new_build", "echo 'Create new build placeholder'", javaVersion),
                    new WorkflowDefinition("create-release.yml", "create-release", "on:\n  workflow_dispatch:", "create_release", "echo 'Create release placeholder'", javaVersion),
                    new WorkflowDefinition("documentation.yml", "documentation", "on:\n  workflow_dispatch:", "documentation", "mvn -B -ntp -DskipTests package", javaVersion),
                    new WorkflowDefinition("security.yml", "security", "on:\n  schedule:\n    - cron: '0 4 * * 1'\n  workflow_dispatch:", "security", "mvn -B -ntp -DskipTests verify", javaVersion),
                    new WorkflowDefinition("sonar-pr.yml", "sonar-pr", "on:\n  pull_request:\n    branches:\n      - main", "sonar_pr", "mvn -B -ntp verify -DskipITs", javaVersion));
        }
    }
}










