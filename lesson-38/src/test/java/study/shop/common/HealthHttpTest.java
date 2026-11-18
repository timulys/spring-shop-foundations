package study.shop.common;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthHttpTest {
    @LocalServerPort int port;
    @ParameterizedTest
    @ValueSource(strings={"liveness","readiness"})
    void internalHealthEndpointReportsStatusWithoutDatabaseDetails(String group) throws Exception {
        try (HttpClient client=HttpClient.newHttpClient()) {
            var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/health/"+group))
                    .GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,response.statusCode());
            var body=JsonMapper.builder().build().readTree(response.body());
            assertEquals("UP",body.get("status").asText());
            assertEquals(1,body.size());
        }
    }
}
