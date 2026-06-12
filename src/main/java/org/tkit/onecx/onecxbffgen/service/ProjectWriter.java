package org.tkit.onecx.onecxbffgen.service;
import org.tkit.onecx.onecxbffgen.model.DependencyProfile;
import org.tkit.onecx.onecxbffgen.model.OperationModel;
import org.tkit.onecx.onecxbffgen.model.SchemaModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
public class ProjectWriter {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}/]+)}");
    private static final String BACKEND_CONFIG_KEY = "backend_api";
    private final TemplateService templateService;
    public ProjectWriter() {
        this(new TemplateService());
    }
    ProjectWriter(TemplateService templateService) {
        this.templateService = templateService;
    }
    public void writePom(Path projectDir,
                         String projectName,
                         String groupId,
                         String artifactId,
                         String parentVersion,
                         DependencyProfile profile,
                         String basePackage,
                         String frontendFileName,
                         String backendApiUrl,
                         String backendApiFileName) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("projectDisplayName", toDisplayName(projectName));
        values.put("parentVersion", parentVersion);
        values.put("groupId", groupId);
        values.put("artifactId", artifactId);
        values.put("packaging", profile == DependencyProfile.MODERN_3_1_PLUS ? "    <packaging>quarkus</packaging>" : "");
        values.put("javaVersion", javaVersion(profile));
        values.put("legacyJunitDependencies", profile == DependencyProfile.LEGACY_UP_TO_2_5
                ? "        <dependency>\n            <groupId>io.quarkus</groupId>\n            <artifactId>quarkus-junit5</artifactId>\n            <scope>test</scope>\n        </dependency>\n        <dependency>\n            <groupId>io.quarkus</groupId>\n            <artifactId>quarkus-junit5-mockito</artifactId>\n            <scope>test</scope>\n        </dependency>\n"
                : profile == DependencyProfile.TRANSITION_2_6_TO_3_0
                ? "        <dependency>\n            <groupId>io.quarkus</groupId>\n            <artifactId>quarkus-junit</artifactId>\n            <scope>test</scope>\n        </dependency>\n        <dependency>\n            <groupId>io.quarkus</groupId>\n            <artifactId>quarkus-junit-mockito</artifactId>\n            <scope>test</scope>\n        </dependency>\n"
                : "");
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
        // Generate download plugin block only for remote URLs; local files are already copied into the project
        String downloadPlugin = "";
        if (backendApiUrl != null && !backendApiUrl.isBlank()) {
            String fn = backendApiFileName != null ? backendApiFileName : "backend-openapi.yaml";
            downloadPlugin = """
            <plugin>
                <groupId>com.googlecode.maven-download-plugin</groupId>
                <artifactId>download-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>download-backend-api</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>wget</goal>
                        </goals>
                        <configuration>
                            <uri>%s</uri>
                            <outputFileName>%s</outputFileName>
                            <outputDirectory>${project.basedir}/target/tmp/openapi</outputDirectory>
                            <overwrite>true</overwrite>
                            <skipCache>true</skipCache>
                        </configuration>
                    </execution>
                </executions>
            </plugin>""".formatted(backendApiUrl, fn);
        }
        values.put("backendApiFileName", backendApiFileName != null ? backendApiFileName : "backend-openapi.yaml");
        values.put("backendDownloadPlugin", downloadPlugin);
        writeTemplate(projectDir.resolve("pom.xml"), "bff-project/pom.xml.tpl", values);
    }
    public void writeGeneratedReadme(Path projectDir,
                                     String projectName,
                                     String groupId,
                                     String basePackage,
                                     String parentVersion,
                                     DependencyProfile profile,
                                     Map<String, List<OperationModel>> controllers) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("projectName", projectName);
        values.put("groupId", groupId);
        values.put("basePackage", basePackage);
        values.put("parentVersion", parentVersion);
        values.put("dependencyProfile", profile.name());
        values.put("javaVersion", javaVersion(profile));
        values.put("curlExamples", buildCurlExamples(controllers));
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
        appValues.put("backendClientBasePackage", "gen." + basePackage + ".client");
        appValues.put("backendConfigKey", BACKEND_CONFIG_KEY);
        writeTemplate(projectDir.resolve("src/main/resources/application.properties"),
                "bff-project/application.properties.tpl", appValues);
        writeTemplate(projectDir.resolve(".gitignore"), "bff-project/gitignore.tpl", Map.of());
        writeTemplate(projectDir.resolve("src/main/helm/Chart.yaml"), "bff-project/Chart.yaml.tpl",
                Map.of("artifactId", artifactId, "projectName", projectName));
        writeTemplate(projectDir.resolve("src/main/helm/values.yaml"), "bff-project/values.yaml.tpl",
                Map.of("artifactId", artifactId,
                        "projectName", projectName,
                        "projectDisplayName", toDisplayName(projectName),
                        "permissionKey", defaultPermissionKey(artifactId),
                        "defaultScopes", "ocx-" + defaultPermissionKey(artifactId) + ":all, ocx-pm:read"));
        writeTemplate(projectDir.resolve("src/main/docker/Dockerfile.jvm"), "bff-project/Dockerfile.jvm.tpl", Map.of());
        writeTemplate(projectDir.resolve("src/main/docker/Dockerfile.native"), "bff-project/Dockerfile.native.tpl", Map.of());
    }

    public void writeWorkflowFiles(Path projectDir, String projectName, DependencyProfile profile) throws IOException {
        GitHubActionsService githubActions = new GitHubActionsService(templateService);
        try {
            githubActions.generate(projectDir, Map.of(
                    "projectName", projectName,
                    "profile", profile.name().toLowerCase()
            ));
        } catch (Exception e) {
            // workflow templates are optional for tests / local generation
            System.err.println("GitHub actions generation failed: " + e.getMessage());
        }
    }

    /**
     * Derives the helm event target repository from the project artifact name.
     * Convention: strip "-bff" suffix and prepend "onecx/".
     * e.g. "onecx-demo-bff" -> "onecx/onecx-demo"
     */
    private String deriveHelmRepo(String projectName) {
        if (projectName == null) return null;
        String base = projectName.endsWith("-bff")
                ? projectName.substring(0, projectName.length() - 4)
                : projectName;
        return "onecx/" + base;
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
            values.put("frontendModelImportStatement", implementFrontendApi
                    ? "import gen." + pkg + ".rs.internal.model.*;"
                    : "");
            values.put("backendModelImportStatement", implementFrontendApi || (!implementFrontendApi && hasAnyRequestBody(entry.getValue()))
                    ? "import gen." + pkg + ".client.model.*;"
                    : "");
            values.put("apiServiceTypeSuffix", implementFrontendApi ? " implements " + controllerBaseName + "ApiService" : "");
            // When implementing frontend API interface, @Path is inherited from the generated interface
            values.put("classPathAnnotation", implementFrontendApi ? "" : "@Path(\"/\")\n");
            values.put("backendClientImport", "gen." + pkg + ".client.api." + backendClientBase + "Api");
            values.put("backendClientType", backendClientBase + "Api");
            values.put("mapperImport", pkg + ".rs.mappers." + mapperType);
            values.put("mapperType", mapperType);
            values.put("methods", buildControllerMethods(entry.getValue(), implementFrontendApi, todoStubMode));
            writeTemplate(baseDir.resolve(controllerName + ".java"), "entity/Controller.java.tpl", values);
        }
    }
    public void writeMapperClasses(Path projectDir, String pkg, List<SchemaModel> frontend, List<SchemaModel> backend,
            Map<String, List<OperationModel>> controllers) throws IOException {
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
                Map.of("packageName", pkg + ".rs.mappers",
                        "internalModelPackage", "gen." + pkg + ".rs.internal.model",
                        "backendClientBasePackage", "gen." + pkg + ".client"));

        // Group all frontend DTOs by their base entity name → one mapper file per entity
        // e.g. Product, SearchProductRequest, CreateProductRequest, UpdateProductRequest → ProductMapper
        Map<String, List<SchemaModel>> frontendByBase = new LinkedHashMap<>();
        for (SchemaModel source : frontend) {
            String normalized = normalizeEntityName(source.name());
            SchemaModel target = backendByNormalized.get(normalized);
            if (target == null || !shouldGenerateMapper(source.name())) continue;
            frontendByBase.computeIfAbsent(normalized, k -> new ArrayList<>()).add(source);
        }

        for (Map.Entry<String, List<SchemaModel>> entry : frontendByBase.entrySet()) {
            String normalized = entry.getKey();
            SchemaModel backendSchema = backendByNormalized.get(normalized);
            if (backendSchema == null) continue;
            String targetType = sanitizeTypeName(backendSchema.name());
            String mapperName = mapperBaseName(sanitizeTypeName(entry.getValue().get(0).name())) + "Mapper";

            // Collect all imports
            Set<String> imports = new LinkedHashSet<>();
            imports.add("org.mapstruct.BeanMapping");
            imports.add("gen." + pkg + ".client.model." + targetType);

            // Build a set of frontend DTO types that have cross-type mappings from operations
            // so we can skip the naive "mainBackendType map(frontendDTO)" for those
            Set<String> crossMappedFrontendTypes = new LinkedHashSet<>();
            for (List<OperationModel> ops : controllers.values()) {
                for (OperationModel op : ops) {
                    String feReqRaw0 = op.requestBodyType();
                    String beReqRaw0 = op.resolvedBackendRequestBodyType();
                    if (feReqRaw0 != null && beReqRaw0 != null && !feReqRaw0.equals(beReqRaw0)) {
                        crossMappedFrontendTypes.add(frontendModelTypeForSchema(sanitizeTypeName(feReqRaw0)));
                    }
                }
            }
            // Detect name collision: frontend DTO and backend type have the same simple name
            // e.g. frontend Category→CategoryDTO, backend Category→Category.
            // Use toBackend/toFrontend named methods to avoid overload ambiguity.
            // Detect name collision: check if any frontend schema has the exact same
            // simple type name as the backend schema (e.g. both named "Category").
            // Only use toBackend/toFrontend when the plain entity names collide,
            // not when only request/response variants match.
            boolean nameCollision = entry.getValue().stream()
                    .anyMatch(s -> sanitizeTypeName(s.name()).equals(sanitizeTypeName(backendSchema.name()))
                            && !sanitizeTypeName(s.name()).toLowerCase().contains("request")
                            && !sanitizeTypeName(s.name()).toLowerCase().contains("response")
                            && !sanitizeTypeName(s.name()).toLowerCase().contains("criteria")
                            && !sanitizeTypeName(s.name()).toLowerCase().contains("search"));

            // Build map() methods for each frontend DTO ↔ backend type
            StringBuilder mapMethods = new StringBuilder();
            Set<String> addedSignatures = new LinkedHashSet<>();
            for (SchemaModel source : entry.getValue()) {
                String sourceType = sanitizeTypeName(source.name());
                String sourceModelType = frontendModelTypeForSchema(sourceType);
                imports.add("gen." + pkg + ".rs.internal.model." + sourceModelType);
                // map DTO → backend (skip for Response DTOs and cross-type mappings)
                if (!sourceType.toLowerCase().contains("response")
                        && !crossMappedFrontendTypes.contains(sourceModelType)) {
                    String sig1 = nameCollision
                            ? targetType + " toBackend(" + sourceModelType + " source)"
                            : targetType + " map(" + sourceModelType + " source)";
                    if (addedSignatures.add(sig1)) {
                        mapMethods.append("    @BeanMapping(ignoreByDefault = true)\n    ").append(sig1).append(";\n");
                    }
                }
                // map backend → DTO (only for plain entity, not Request/Criteria/Response)
                if (!sourceType.toLowerCase().contains("request") && !sourceType.toLowerCase().contains("criteria")
                        && !sourceType.toLowerCase().contains("search") && !sourceType.toLowerCase().contains("response")) {
                    String sig2 = nameCollision
                            ? sourceModelType + " toFrontend(" + targetType + " source)"
                            : sourceModelType + " map(" + targetType + " source)";
                    if (addedSignatures.add(sig2)) {
                        mapMethods.append("    @BeanMapping(ignoreByDefault = true)\n    ").append(sig2).append(";\n");
                    }
                }
            }
            // Ensure the plain backend→DTO mapping is covered.
            String plainDtoType = frontendModelTypeForSchema(sanitizeTypeName(backendSchema.name()));
            imports.add("gen." + pkg + ".rs.internal.model." + plainDtoType);
            String sigPlain = nameCollision
                    ? plainDtoType + " toFrontend(" + targetType + " source)"
                    : plainDtoType + " map(" + targetType + " source)";
            addedSignatures.add(sigPlain);
            if (!mapMethods.toString().contains(sigPlain + ";")) {
                mapMethods.append("    @BeanMapping(ignoreByDefault = true)\n    ").append(sigPlain).append(";\n");
            }

            // --- cross-type mappings from operations ---
            // E.g. SearchProductRequestDTO → ProductSearchCriteria (not Product)
            //      ProductPageResult → SearchProductResponseDTO
            for (List<OperationModel> ops : controllers.values()) {
                for (OperationModel op : ops) {
                    // request body cross-mapping: frontendRequestType → backendRequestType
                    String feReqRaw = op.requestBodyType();
                    String beReqRaw = op.resolvedBackendRequestBodyType();
                    if (feReqRaw != null && beReqRaw != null && !feReqRaw.equals(beReqRaw)) {
                        String feReqType = frontendModelTypeForSchema(sanitizeTypeName(feReqRaw));
                        String beReqType = backendModelTypeForSchema(sanitizeTypeName(beReqRaw));
                        // only add to THIS mapper if feReqType belongs to this entity group (by normalizeEntityName)
                        if (normalizeEntityName(sanitizeTypeName(feReqRaw)).equals(normalized)
                                || normalizeEntityName(sanitizeTypeName(beReqRaw)).equals(normalized)) {
                            imports.add("gen." + pkg + ".rs.internal.model." + feReqType);
                            imports.add("gen." + pkg + ".client.model." + beReqType);
                            String crossSig = beReqType + " map(" + feReqType + " source)";
                            if (addedSignatures.add(crossSig)) {
                                mapMethods.append("    @BeanMapping(ignoreByDefault = true)\n    ").append(crossSig).append(";\n");
                            }
                        }
                    }
                    // response cross-mapping: backendResponseType → frontendResponseType
                    // Use named mapper method to avoid duplicate map(BackendType) overloads
                    String feRespRaw = op.responseType();
                    String beRespRaw = op.resolvedBackendResponseType();
                    if (feRespRaw != null && beRespRaw != null
                            && !sanitizeTypeName(feRespRaw).equals(sanitizeTypeName(beRespRaw))) {
                        String feRespType = frontendModelTypeForSchema(sanitizeTypeName(feRespRaw));
                        String beRespType = backendModelTypeForSchema(sanitizeTypeName(beRespRaw));
                        if (normalizeEntityName(sanitizeTypeName(feRespRaw)).equals(normalized)
                                || normalizeEntityName(sanitizeTypeName(beRespRaw)).equals(normalized)) {
                            imports.add("gen." + pkg + ".rs.internal.model." + feRespType);
                            imports.add("gen." + pkg + ".client.model." + beRespType);
                            // Named method: toSearchProductResponse, toCreateProductResponse, etc.
                            String namedMethod = responseMapperMethodName(op.operationId(), feRespRaw);
                            String namedSig = "@BeanMapping(ignoreByDefault = true)\n    " + feRespType + " " + namedMethod + "(" + beRespType + " source)";
                            if (addedSignatures.add(namedMethod)) {
                                mapMethods.append("    ").append(namedSig).append(";\n");
                            }
                        }
                    }
                }
            }
            // --- end cross-type mappings ---
            String importStatements = imports.stream()
                    .map(i -> "import " + i + ";")
                    .collect(java.util.stream.Collectors.joining("\n"));

            Map<String, String> values = new LinkedHashMap<>();
            values.put("packageName", pkg + ".rs.mappers");
            values.put("sourceImportStatement", importStatements);
            values.put("targetImportStatement", "");
            values.put("className", mapperName);
            values.put("mapMethods", mapMethods.toString());
            values.put("usesClause", buildMapperUsesClause(entry.getValue().get(0), normalizedToMapper));
            writeTemplate(baseDir.resolve(mapperName + ".java"), "entity/Mapper.java.tpl", values);
        }
    }
    public void writeTestScaffold(Path projectDir, String pkg, String artifactId,
                                   Set<String> permissionKeys,
                                   Map<String, List<OperationModel>> controllers,
                                   Map<String, String> backendClientByController) throws IOException {
        Path baseDir = projectDir.resolve("src/test/java/" + pkg.replace('.', '/') + "/rs");
        writeTemplate(baseDir.resolve("AbstractTest.java"), "test/AbstractTest.java.tpl",
                Map.of("packageName", pkg + ".rs"));
        for (String key : new TreeMap<>(controllers).keySet()) {
            String controllerBaseName = sanitizeTypeName(key);
            String controllerName = controllerBaseName + "RestController";
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
        writeTemplate(resourcesDir.resolve("mockserver/permissions.json"), "test/mockserver-permissions.json.tpl",
                Map.of("artifactId", artifactId,
                        "alicePermissions", buildPermissionsJsonBlock(permissionKeys, artifactId, List.of("read", "write", "delete")),
                        "bobPermissions", buildPermissionsJsonBlock(permissionKeys, artifactId, List.of("read"))));
    }
    private String buildControllerMethods(List<OperationModel> operations, boolean implementFrontendApi, boolean todoStubMode) {
        StringBuilder sb = new StringBuilder();
        for (OperationModel op : operations) {
            List<String> params = pathParams(op.path());
            String signature = methodSignature(params);

            // if request body, add dto param
            String bodyParam = null;
            String dtoType = null;
            if (op.hasRequestBody()) {
                dtoType = implementFrontendApi
                        ? frontendModelTypeForSchema(op.requestBodyType())
                        : backendModelTypeForSchema(op.requestBodyType());
                bodyParam = buildRequestBodyParamName(op.requestBodyType());
                String bodyParamDecl = dtoType + " " + bodyParam;
                signature = signature.isBlank() ? bodyParamDecl : signature + ", " + bodyParamDecl;
            }

            if (!implementFrontendApi) {
                sb.append("    @").append(op.httpMethod()).append("\n");
                sb.append("    @Path(\"").append(op.path()).append("\")\n");
                if (op.hasRequestBody()) {
                    sb.append("    @Consumes(MediaType.APPLICATION_JSON)\n");
                }
                if (op.hasResponseBody()) {
                    sb.append("    @Produces(MediaType.APPLICATION_JSON)\n");
                }
            } else {
                sb.append("    @Override\n");
            }
            sb
                    .append("    public Response ").append(sanitizeMethodName(op.operationId())).append("(")
                    .append(signature).append(") {\n");

            if (todoStubMode) {
                sb.append("        // TODO implement backend call and mapper conversion.\n")
                        .append("        return null;\n");
            } else {
                // Build backend client call args
                List<String> callArgs = new ArrayList<>(params.stream().map(this::sanitizeFieldName).toList());
                if (bodyParam != null) {
                    if (implementFrontendApi) {
                        // map frontend DTO → backend model
                        String backendBodyType = backendModelTypeForSchema(op.resolvedBackendRequestBodyType() != null
                                ? op.resolvedBackendRequestBodyType() : op.requestBodyType());
                        callArgs.add("mapper.map(" + bodyParam + ")");
                        // reassign dtoType so signature uses frontend DTO
                    } else {
                        callArgs.add(bodyParam);
                    }
                }
                String backendOpId = implementFrontendApi
                        ? sanitizeMethodName(op.resolvedBackendOperationId() != null ? op.resolvedBackendOperationId() : op.operationId())
                        : sanitizeMethodName(op.operationId());
                String clientCall = "client." + backendOpId + "(" + String.join(", ", callArgs) + ")";

                String responseType = implementFrontendApi
                        ? op.resolvedBackendResponseType()
                        : op.responseType();

                if (responseType == null || responseType.isBlank()) {
                    // no response body — DELETE / fire-and-forget style
                    if (implementFrontendApi) {
                        sb.append("        try (Response backendResponse = ").append(clientCall).append(") {\n")
                          .append("            return Response.status(backendResponse.getStatus()).build();\n")
                          .append("        }\n");
                    } else {
                        sb.append("        ").append(clientCall).append(";\n")
                                .append("        return Response.noContent().build();\n");
                    }
                } else if (implementFrontendApi) {
                    String backendType = backendModelTypeForSchema(responseType);
                    String frontendResponseType = frontendModelTypeForSchema(op.responseType());
                    // Determine mapper method name: named method if types differ, else plain map()
                    String feRespRaw = op.responseType();
                    String beRespRaw = op.resolvedBackendResponseType();
                    String mapperCall;
                    if (feRespRaw != null && beRespRaw != null
                            && !sanitizeTypeName(feRespRaw).equals(sanitizeTypeName(beRespRaw))) {
                        // Use named method e.g. mapper.toSearchProductResponse(result)
                        mapperCall = "mapper." + responseMapperMethodName(op.operationId(), feRespRaw) + "(result)";
                    } else {
                        mapperCall = "mapper.map(result)";
                    }
                    sb.append("        try (Response backendResponse = ").append(clientCall).append(") {\n")
                      .append("            ").append(backendType).append(" result = backendResponse.readEntity(").append(backendType).append(".class);\n")
                      .append("            return Response.status(backendResponse.getStatus()).entity(").append(mapperCall).append(").build();\n")
                      .append("        }\n");
                } else {
                    // fallback: backend client returns Response, propagate directly
                    int successCode = op.successStatusCode() > 0 ? op.successStatusCode() : 200;
                    String entityType = backendModelTypeForSchema(responseType);
                    sb.append("        try (Response backendResponse = ").append(clientCall).append(") {\n")
                      .append("            ").append(entityType).append(" result = backendResponse.readEntity(").append(entityType).append(".class);\n")
                      .append("            return Response.status(").append(successCode).append(").entity(result).build();\n")
                      .append("        }\n");
                }
            }
            sb.append("    }\n\n");
        }
        return sb.toString();
    }

    private String buildEndpointTests(List<OperationModel> operations) {
        StringBuilder sb = new StringBuilder();
        for (OperationModel op : operations) {
            String methodName = sanitizeMethodName(op.operationId());
            String testName = methodName + "Test";
            String httpMethod = op.httpMethod();
            String opPath = op.path();
            boolean hasBody = op.hasRequestBody();
            boolean hasResponse = op.hasResponseBody();
            List<String> pathParamNames = pathParams(opPath);
            int successStatus = resolveSuccessStatus(op.successStatusCode(), httpMethod, hasResponse);
            // Build RestAssured path: replace {param} with "test-id" literals (uses frontend path)
            String restAssuredPath = opPath;
            for (String p : pathParamNames) {
                restAssuredPath = restAssuredPath.replace("{" + p + "}", "test-id");
            }
            // Mock server path uses BACKEND path (the actual URL the client calls)
            String backendOpPath = op.resolvedBackendPath() != null ? op.resolvedBackendPath() : opPath;
            String mockServerPath = backendOpPath;
            List<String> backendPathParams = pathParams(backendOpPath);
            for (String p : backendPathParams) {
                mockServerPath = mockServerPath.replace("{" + p + "}", "test-id");
            }
            sb.append("    @Test\n")
              .append("    void ").append(testName).append("() {\n");
            // unauthorized
            sb.append("        // unauthorized\n")
              .append("        given()\n")
              .append("                .when()\n");
            if (hasBody) {
                sb.append("                .contentType(APPLICATION_JSON)\n")
                  .append("                .body(\"{}\")\n");
            }
            sb.append("                .").append(httpMethod.toLowerCase(Locale.ROOT)).append("(\"").append(restAssuredPath).append("\")\n")
              .append("                .then()\n")
              .append("                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());\n\n");
            // mock backend + authorized
            sb.append("        // mock backend\n")
              .append("        mockServerClient.when(\n")
              .append("                HttpRequest.request()\n")
              .append("                        .withPath(\"").append(mockServerPath).append("\")\n")
              .append("                        .withMethod(\"").append(httpMethod).append("\"))\n")
              .append("                .withId(MOCK_ID).respond(\n")
              .append("                        HttpResponse.response()\n")
              .append("                                .withStatusCode(").append(successStatus).append(")");
            if (hasResponse) {
                sb.append("\n                                .withHeader(\"Content-Type\", \"application/json\")\n")
                  .append("                                .withBody(\"{}\")");
            }
            sb.append(");\n\n");
            sb.append("        given()\n")
              .append("                .when()\n")
              .append("                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))\n")
              .append("                .header(APM_HEADER_PARAM, ADMIN)\n");
            if (hasBody) {
                sb.append("                .contentType(APPLICATION_JSON)\n")
                  .append("                .body(\"{}\")\n");
            }
            sb.append("                .").append(httpMethod.toLowerCase(Locale.ROOT)).append("(\"").append(restAssuredPath).append("\")\n")
              .append("                .then()\n")
              .append("                .statusCode(").append(successStatus).append(");\n");
            sb.append("    }\n\n");
        }
        return sb.toString();
    }
    private int resolveSuccessStatus(int openApiStatusCode, String httpMethod, boolean hasResponse) {
        if (openApiStatusCode > 0) {
            return openApiStatusCode;
        }
        if ("DELETE".equals(httpMethod) || !hasResponse) {
            return 204;
        }
        if ("POST".equals(httpMethod)) {
            return 201;
        }
        return 200;
    }
    private String buildMockitoAnyArgs(int count) {
        if (count == 0) {
            return "";
        }
        return String.join(", ", java.util.Collections.nCopies(count, "org.mockito.ArgumentMatchers.any()"));
    }
    private List<String> pathParams(String path) {
        if (path == null) return List.of();
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
    private String methodSignature(List<String> params) {
        return String.join(", ", params.stream()
                .map(name -> "String " + sanitizeFieldName(name))
                .toList());
    }
    private void writeTemplate(Path path, String templateName, Map<String, String> values) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, templateService.render(templateName, values));
    }

    private String buildCurlExamples(Map<String, List<OperationModel>> controllers) {
        if (controllers == null || controllers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("## API Examples (curl)\n\n");
        sb.append("Replace `TOKEN` with a valid Bearer token.\n\n");
        for (Map.Entry<String, List<OperationModel>> entry : new TreeMap<>(controllers).entrySet()) {
            String tag = sanitizeTypeName(entry.getKey());
            sb.append("### ").append(tag).append("\n\n");
            for (OperationModel op : entry.getValue()) {
                String path = op.path();
                sb.append("#### ").append(sanitizeMethodName(op.operationId())).append("\n\n");
                sb.append("```bash\n");
                String examplePath = path.replaceAll("\\{[^}]+}", "example-id");
                String nl = " \\\\\n";
                String curlBase = "curl -s -X " + op.httpMethod() + nl
                        + "  http://localhost:8080" + examplePath + nl
                        + "  -H 'Authorization: Bearer TOKEN'" + nl
                        + "  -H 'apm-principal-token: TOKEN'";
                if (op.hasRequestBody()) {
                    String requestBodyType = op.requestBodyType();
                    String exampleBody;
                    if (requestBodyType != null && requestBodyType.toLowerCase().contains("search")) {
                        exampleBody = "{\"pageNumber\": 0, \"pageSize\": 10}";
                    } else {
                        exampleBody = "{\"name\": \"example\"}";
                    }
                    curlBase += nl + "  -H 'Content-Type: application/json'" + nl + "  -d '" + exampleBody + "'";
                }
                sb.append(curlBase).append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    private String javaVersion(DependencyProfile profile) {
        return profile == DependencyProfile.MODERN_3_1_PLUS ? "25" : "17";
    }

    /**
     * Converts a kebab-case artifact name to a human-readable display name.
     * Expands known abbreviations: "onecx" -> "OneCX", "bff" -> "Backend For Frontend", "svc" -> "Service".
     * e.g. "onecx-demo-bff" -> "OneCX Demo Backend For Frontend"
     */
    private String toDisplayName(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) return artifactId;
        String[] parts = artifactId.split("[-.]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            switch (part.toLowerCase()) {
                case "onecx" -> sb.append("OneCX");
                case "bff"   -> sb.append("Backend For Frontend");
                case "svc"   -> sb.append("Service");
                default      -> sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }
    private String sanitizeTypeName(String value) {
        if (value == null) return "GeneratedType";
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
        if (value == null) return "field";
        String cleaned = value.replaceAll("[^a-zA-Z0-9]", " ").trim();
        if (cleaned.isBlank()) {
            return "field";
        }
        // If already a single camelCase/identifier token (no spaces produced), preserve as-is
        // just ensure first char is lowercase
        if (!cleaned.contains(" ")) {
            String result = Character.toLowerCase(cleaned.charAt(0)) + cleaned.substring(1);
            if (Character.isDigit(result.charAt(0))) {
                result = "field" + result;
            }
            return result;
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

    private String frontendModelTypeForSchema(String schemaType) {
        if (schemaType == null || schemaType.isBlank()) {
            return schemaType;
        }
        String normalized = schemaType.trim();
        if (normalized.startsWith("List<") && normalized.endsWith(">")) {
            String inner = normalized.substring(5, normalized.length() - 1).trim();
            return "List<" + frontendModelTypeForSchema(inner) + ">";
        }
        if ("Object".equals(normalized)) {
            return normalized;
        }
        String sanitized = sanitizeTypeName(normalized);
        // Avoid double DTO suffix (schema may already end with DTO)
        if (sanitized.endsWith("DTO")) {
            return sanitized;
        }
        return sanitized + "DTO";
    }

    private String backendModelTypeForSchema(String schemaType) {
        if (schemaType == null || schemaType.isBlank()) {
            return schemaType;
        }
        String normalized = schemaType.trim();
        if (normalized.startsWith("List<") && normalized.endsWith(">")) {
            String inner = normalized.substring(5, normalized.length() - 1).trim();
            return "List<" + backendModelTypeForSchema(inner) + ">";
        }
        if ("Object".equals(normalized)) {
            return normalized;
        }
        return sanitizeTypeName(normalized);
    }

    private boolean hasAnyRequestBody(List<OperationModel> operations) {
        return operations.stream().anyMatch(OperationModel::hasRequestBody);
    }

    private String buildRequestBodyParamName(String schemaType) {
        if (schemaType == null || schemaType.isBlank()) {
            return "requestDto";
        }
        String normalized = schemaType.trim();
        if (normalized.startsWith("List<") && normalized.endsWith(">")) {
            normalized = normalized.substring(5, normalized.length() - 1).trim();
        }
        String baseName = sanitizeTypeName(normalized).replaceAll("(DTO)+$", "");
        if (baseName.isBlank()) {
            return "requestDto";
        }
        return sanitizeFieldName(baseName) + "Dto";
    }
    /**
     * Normalizes an entity name for mapper grouping purposes.
     * Strips suffixes (Dto, Response, Request, Internal, External) and also
     * leading verb prefixes (Search, Create, Update, Delete, Get) so that
     * SearchProductRequest, CreateProductRequest, and Product all map to "product".
     */
    /**
     * Derives a named mapper method for response conversion.
     * e.g. operationId="searchProductItems", feRespRaw="SearchProductResponse"
     *      → "toSearchProductResponse"
     * e.g. operationId="createProduct", feRespRaw="CreateProductResponse"
     *      → "toCreateProductResponse"
     */
    private String responseMapperMethodName(String operationId, String feRespRaw) {
        String typePart = sanitizeTypeName(feRespRaw != null ? feRespRaw : operationId);
        return "to" + typePart;
    }
    private String normalizeEntityName(String value) {
        String stripped = sanitizeTypeName(value)
                .replaceAll("(Dto|DTO|Response|Request|Internal|External)$", "");
        // Also strip leading verb prefixes
        stripped = stripped.replaceAll("^(Search|Create|Update|Delete|Get|List|Fetch|Find)(.+)$", "$2");
        return stripped.toLowerCase(Locale.ROOT);
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
    private String buildPermissionsJsonBlock(Set<String> keys, String artifactId, List<String> actions) {
        Set<String> resolved = (keys == null || keys.isEmpty())
                ? Set.of(defaultPermissionKey(artifactId))
                : keys;
        String actionList = actions.stream().map(a -> "\"" + a + "\"").collect(Collectors.joining(", "));
        return resolved.stream()
                .map(k -> "\"" + k + "\": [" + actionList + "]")
                .collect(Collectors.joining(",\n            "));
    }

    private String toPropertyToken(String value) {
        String normalized = value.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return normalized.toLowerCase(Locale.ROOT);
    }
    private String defaultPermissionKey(String artifactId) {
        String[] tokens = artifactId.split("[-.]");
        for (String token : tokens) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (!lower.isBlank() && !"onecx".equals(lower) && !"bff".equals(lower)) {
                return lower;
            }
        }
        return "resource";
    }
}






































