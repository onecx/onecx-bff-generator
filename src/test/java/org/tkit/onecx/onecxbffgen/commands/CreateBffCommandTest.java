package org.tkit.onecx.onecxbffgen.commands;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class CreateBffCommandTest {
    @Test
    void shouldGenerateProjectWithModernProfile() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-modern-");
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "demo-bff",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "3.1.0"
        );
        Path generated = tempDir.resolve("demo-bff");
        assertEquals(0, result);
        assertTrue(Files.exists(generated.resolve("pom.xml")));
        assertTrue(Files.exists(generated.resolve("README.md")));
        assertTrue(Files.exists(generated.resolve("src/main/openapi/frontend.yaml")));
        assertTrue(Files.exists(generated.resolve("target/tmp/openapi/backend.yaml")), "Local backend spec should be copied into target/tmp/openapi");
        assertFalse(Files.exists(generated.resolve("src/main/openapi/openapi-frontend.yaml")));
        assertFalse(Files.exists(generated.resolve("target/tmp/openapi/openapi-backend.yaml")));
        assertTrue(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/controllers/UsersRestController.java")));
        assertTrue(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/mappers/UserMapper.java")));
        assertTrue(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/mappers/ExceptionMapper.java")));
        assertTrue(Files.exists(generated.resolve("src/test/java/org/tkit/onecx/demo/bff/rs/UsersRestControllerTest.java")));
        assertTrue(Files.exists(generated.resolve("src/test/java/org/tkit/onecx/demo/bff/rs/UsersRestControllerIT.java")));
        assertTrue(Files.exists(generated.resolve("src/test/resources/mockserver.properties")));
        assertTrue(Files.exists(generated.resolve("src/main/helm/Chart.yaml")));
        assertTrue(Files.exists(generated.resolve("src/main/helm/values.yaml")));
        assertTrue(Files.exists(generated.resolve("src/main/docker/Dockerfile.jvm")));
        assertTrue(Files.exists(generated.resolve("src/main/docker/Dockerfile.native")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build-branch.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build-pr.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build-pr-merge.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build-release.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/create-fix-branch.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/create-new-build.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/create-release.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/documentation.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/security.yml")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/sonar-pr.yml")));
        assertTrue(Files.exists(generated.resolve(".github/dependabot.yml")));
        String pom = Files.readString(generated.resolve("pom.xml"));
        assertTrue(pom.contains("<packaging>quarkus</packaging>"));
        assertFalse(pom.contains("<maven.compiler.release>"));
        assertTrue(pom.contains("<artifactId>quarkus-openapi-generator</artifactId>"));
        assertTrue(pom.contains("<artifactId>tkit-quarkus-rest-context</artifactId>"));
        assertTrue(pom.contains("<artifactId>onecx-permissions</artifactId>"));
        assertFalse(pom.contains("<artifactId>maven-surefire-plugin</artifactId>"));
        assertFalse(pom.contains("SecurityDynamicImplTest"));
        assertFalse(pom.contains("<artifactId>swagger-parser</artifactId>"));
        assertFalse(pom.contains("<artifactId>tkit-quarkus-security</artifactId>"));
        assertTrue(pom.contains("<name>Demo Backend For Frontend</name>"));
        assertFalse(pom.contains("download-maven-plugin"), "pom.xml should NOT contain download plugin when backend is a local file");
        assertFalse(pom.contains("download-backend-api"), "pom.xml should NOT contain download-backend-api when backend is a local file");
        // Local backend file is copied into project, not referenced in pom.xml download plugin
        String usersController = Files.readString(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/controllers/UsersRestController.java"));
        assertTrue(usersController.contains("UserDTO"), "Frontend DTO type should be used in controller method signature");
        assertFalse(usersController.contains("UserDTODTO"), "DTO suffix should not be doubled");
        String usersTest = Files.readString(generated.resolve("src/test/java/org/tkit/onecx/demo/bff/rs/UsersRestControllerTest.java"));
        assertTrue(usersTest.contains("@QuarkusTest"), "Test should be a QuarkusTest");
        assertTrue(usersTest.contains("keycloakClient.getAccessToken"), "Test should use Keycloak auth");
        assertTrue(usersTest.contains("MockServerClient"), "Test should use MockServerClient");
        assertTrue(usersTest.contains("UNAUTHORIZED"), "Test should verify 401");
        String abstractTest = Files.readString(generated.resolve("src/test/java/org/tkit/onecx/demo/bff/rs/AbstractTest.java"));
        assertTrue(abstractTest.contains("KeycloakTestClient"), "AbstractTest should have KeycloakTestClient");
        assertTrue(abstractTest.contains("MockServerTestResource"), "AbstractTest should register MockServerTestResource");
        // @InjectMockServerClient is declared in each test class (not abstract base)
        assertTrue(usersTest.contains("@InjectMockServerClient"), "Test class should inject MockServerClient");
    }
    @Test
    void shouldGenerateProjectWithLegacyProfile() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-legacy-");
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "demo-bff-legacy",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "2.5.0"
        );
        Path generated = tempDir.resolve("demo-bff-legacy");
        assertEquals(0, result);
        String pom = Files.readString(generated.resolve("pom.xml"));
        assertFalse(pom.contains("<packaging>quarkus</packaging>"));
        assertTrue(pom.contains("<artifactId>quarkus-junit5</artifactId>"));
        assertTrue(pom.contains("<artifactId>swagger-parser</artifactId>"));
        assertTrue(pom.contains("<artifactId>quarkus-test-keycloak-server</artifactId>"));
    }
    @Test
    void shouldGenerateProjectWithTransitionProfile() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-transition-");
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "demo-bff-transition",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "2.6.0"
        );
        Path generated = tempDir.resolve("demo-bff-transition");
        assertEquals(0, result);
        String pom = Files.readString(generated.resolve("pom.xml"));
        assertFalse(pom.contains("<packaging>quarkus</packaging>"));
        assertTrue(pom.contains("<artifactId>quarkus-junit</artifactId>"));
        assertTrue(pom.contains("<artifactId>quarkus-junit-mockito</artifactId>"));
        assertFalse(pom.contains("<artifactId>swagger-parser</artifactId>"));
        assertTrue(pom.contains("quarkus-test-keycloak-server"));
    }
    @Test
    void shouldAvoidDuplicatedOnecxPackageSegmentForOnecxPrefixedArtifact() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-package-");
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "onecx-demo-bff",
                "--group-id", "org.tkit.onecx",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "3.1.0"
        );
        Path generated = tempDir.resolve("onecx-demo-bff");
        assertEquals(0, result);
        assertTrue(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/controllers/UsersRestController.java")));
        assertFalse(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/onecx/demo/bff/rs/controllers/UsersRestController.java")));
    }
    @Test
    void shouldGenerateInPlaceWhenOutputDirAlreadyPointsToArtifactFolder() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-in-place-");
        Path projectDir = tempDir.resolve("onecx-demo-bff");
        Files.createDirectories(projectDir);
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "onecx-demo-bff",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", projectDir.toString(),
                "--parent-version", "3.1.0"
        );
        assertEquals(0, result);
        assertTrue(Files.exists(projectDir.resolve("pom.xml")));
        assertFalse(Files.exists(projectDir.resolve("onecx-demo-bff/pom.xml")));
    }
    @Test
    void shouldUseNameGroupAndPackageInputsInGeneratedConfig() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-name-group-package-");
        Path frontend = Path.of("src/test/resources/openapi/frontend.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend.yaml").toAbsolutePath();
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--name", "onecx-demo-bff",
                "--group", "org.tkit.onecx",
                "--package", "org.tkit.onecx.demo",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "3.1.0"
        );
        Path generated = tempDir.resolve("onecx-demo-bff");
        assertEquals(0, result);
        String pom = Files.readString(generated.resolve("pom.xml"));
        assertTrue(pom.contains("<groupId>org.tkit.onecx</groupId>"));
        assertTrue(pom.contains("<artifactId>onecx-demo-bff</artifactId>"));
        assertTrue(pom.contains("<name>OneCX Demo Backend For Frontend</name>"));
        String appProps = Files.readString(generated.resolve("src/main/resources/application.properties"));
        assertTrue(appProps.contains("onecx.generator.name=onecx-demo-bff"));
        assertTrue(appProps.contains("onecx.generator.group=org.tkit.onecx"));
        assertTrue(appProps.contains("onecx.generator.package=org.tkit.onecx.demo.bff"));
        assertTrue(appProps.contains("onecx.permissions.application-id=${quarkus.application.name}"));
        assertTrue(appProps.contains("quarkus.openapi-generator.codegen.input-base-dir=target/tmp/openapi"));
        assertTrue(Files.exists(generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/controllers/UsersRestController.java")));
    }

    @Test
    void shouldGenerateUnambiguousMapperWhenFrontendAndBackendTypesShareSimpleName() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-mapper-collision-");
        Path frontend = Path.of("src/test/resources/openapi/frontend-collision.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend-collision.yaml").toAbsolutePath();

        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--name", "onecx-demo-bff",
                "--group", "org.tkit.onecx",
                "--package", "org.tkit.onecx.demo",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "3.1.0"
        );

        Path generated = tempDir.resolve("onecx-demo-bff");
        Path mapperPath = generated.resolve("src/main/java/org/tkit/onecx/demo/bff/rs/mappers/CategoryMapper.java");
        assertEquals(0, result);
        assertTrue(Files.exists(mapperPath));

        String mapper = Files.readString(mapperPath);
        assertFalse(mapper.contains("import org.tkit.onecx.demo.bff.domain.model.frontend.Category;\nimport org.tkit.onecx.demo.bff.domain.model.backend.Category;"));
        assertTrue(mapper.contains("import gen.org.tkit.onecx.demo.bff.rs.internal.model.CategoryDTO;"));
        assertTrue(mapper.contains("import gen.org.tkit.onecx.demo.bff.client.model.Category;"));
        assertTrue(mapper.contains("Category toBackend(CategoryDTO source);"));
        assertTrue(mapper.contains("CategoryDTO toFrontend(Category source);"));
    }

    @Test
    void shouldGenerateFallbackControllerWithBackendModelImportsWhenFrontendHasNoPaths() throws Exception {
        Path tempDir = Files.createTempDirectory("bff-generator-fallback-");
        Path frontend = Path.of("src/test/resources/openapi/frontend-no-paths.yaml").toAbsolutePath();
        Path backend = Path.of("src/test/resources/openapi/backend-product.yaml").toAbsolutePath();
        
        int result = new CommandLine(new CreateBffCommand(new org.tkit.onecx.onecxbffgen.service.GeneratorService())).execute(
                "--project-name", "demo-bff-fallback",
                "--frontend-api", frontend.toString(),
                "--backend-api", backend.toString(),
                "--output-dir", tempDir.toString(),
                "--parent-version", "3.1.0"
        );
        
        Path generated = tempDir.resolve("demo-bff-fallback");
        assertEquals(0, result);
        
        Path controllerPath = generated.resolve("src/main/java/org/tkit/onecx/demo/bff/fallback/rs/controllers/ProductRestController.java");
        assertTrue(Files.exists(controllerPath));
        
        String controller = Files.readString(controllerPath);
        // In fallback mode (frontend has no paths), backend model imports should be present
        assertTrue(controller.contains("import gen.org.tkit.onecx.demo.bff.fallback.client.model.*;"),
                "Controller should import backend model types for fallback mode");
        // Request/response types should use backend model names exactly as they appear in backend schema
        assertTrue(controller.contains("ProductSearchCriteriaDTO"),
                "Controller method parameter should use backend schema type");
    }
}
