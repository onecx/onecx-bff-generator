package ${packageName};
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
@QuarkusTest
class ${className}Test extends AbstractTest {
    @InjectMockServerClient
    MockServerClient mockServerClient;
    @AfterEach
    void resetMocks() {
        mockServerClient.clear(MOCK_ID);
    }
${endpointTests}}
