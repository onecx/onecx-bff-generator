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
        assertTrue(Files.exists(generated.resolve("src/main/openapi/frontend/frontend.yaml")));
        assertTrue(Files.exists(generated.resolve("src/main/openapi/backend/backend.yaml")));
        assertFalse(Files.exists(generated.resolve("src/main/openapi/frontend/openapi-frontend.yaml")));
        assertFalse(Files.exists(generated.resolve("src/main/openapi/backend/openapi-backend.yaml")));
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
        assertTrue(Files.exists(generated.resolve("demo-bff.adoc")));
        assertTrue(Files.exists(generated.resolve(".github/workflows/build.yml")));
        String pom = Files.readString(generated.resolve("pom.xml"));
        assertTrue(pom.contains("<packaging>quarkus</packaging>"));
        assertTrue(pom.contains("<maven.compiler.release>25</maven.compiler.release>"));
        assertTrue(pom.contains("<artifactId>quarkus-openapi-generator</artifactId>"));
        assertTrue(pom.contains("<artifactId>tkit-quarkus-rest-context</artifactId>"));
        assertTrue(pom.contains("<artifactId>onecx-permissions</artifactId>"));
        assertFalse(pom.contains("<artifactId>swagger-parser</artifactId>"));
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
        assertTrue(pom.contains("<maven.compiler.release>17</maven.compiler.release>"));
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
        assertTrue(pom.contains("<maven.compiler.release>17</maven.compiler.release>"));
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
        assertTrue(pom.contains("<name>onecx-demo-bff</name>"));
        String appProps = Files.readString(generated.resolve("src/main/resources/application.properties"));
        assertTrue(appProps.contains("onecx.generator.name=onecx-demo-bff"));
        assertTrue(appProps.contains("onecx.generator.group=org.tkit.onecx"));
        assertTrue(appProps.contains("onecx.generator.package=org.tkit.onecx.demo.bff"));
        assertTrue(appProps.contains("onecx.permissions.application-id=${quarkus.application.name}"));
        assertTrue(appProps.contains("quarkus.openapi-generator.codegen.input-base-dir=src/main/openapi/backend"));
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
        assertTrue(mapper.contains("import gen.org.tkit.onecx.demo.bff.backend.client.model.Category;"));
        assertTrue(mapper.contains("Category toBackend(CategoryDTO source);"));
        assertTrue(mapper.contains("CategoryDTO toFrontend(Category source);"));
    }
}




