package ${packageName};
import ${controllerImport};
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@QuarkusTest
class ${className}Test extends AbstractTest {
    @Inject
    ${className} controller;
    @Test
    void shouldInjectController() {
        assertNotNull(controller);
    }

${endpointTests}
}
