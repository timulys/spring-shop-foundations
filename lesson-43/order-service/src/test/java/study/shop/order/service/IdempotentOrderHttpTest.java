package study.shop.order.service;

import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotentOrderHttpTest extends study.shop.common.CatalogServerTestBase {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = study.shop.common.CsrfTestClient.client();
    private final JsonMapper json = JsonMapper.builder().build();
    @BeforeEach void login() throws Exception { study.shop.common.OwnerTestData.loginFixture(client, port, jdbc); }
    @AfterEach void close() { client.close(); }

    private HttpResponse<String> request(String method, String path, String body, String header, String value) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (header != null) builder.header(header, value);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return study.shop.common.CsrfTestClient.send(client, builder.build(), false);
    }
    private String prepare() throws Exception {
        var add = request("POST", "/api/cart/items", "{\"productCode\":\"P001\",\"quantity\":2}", "X-Cart-Revision", "0");
        assertEquals(200, add.statusCode()); assertEquals(1, json.readTree(add.body()).get("revision").asLong());
        return "{\"cartRevision\":1,\"expectedTotalWon\":55000}";
    }
    private void error(HttpResponse<String> response, int status, String code) {
        assertEquals(status, response.statusCode(), response.body()); assertEquals(code, json.readTree(response.body()).get("code").asText());
    }

    @Test void missingOrderKeyIs400AndSavesNothing() throws Exception {
        error(request("POST", "/api/orders", prepare(), null, null), 400, "INVALID_IDEMPOTENCY_KEY");
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from cart_items", Integer.class));
    }
    @Test void identicalHttpReplayKeepsOrderAndLocationDuringCatalogFailure() throws Exception {
        String body = prepare(), key = UUID.randomUUID().toString();
        var first = request("POST", "/api/orders", body, "Idempotency-Key", key);
        assertEquals(201, first.statusCode(), first.body()); CATALOG.failAll = true;
        var second = request("POST", "/api/orders", body, "Idempotency-Key", key);
        assertEquals(201, second.statusCode()); assertEquals(json.readTree(first.body()), json.readTree(second.body()));
        assertEquals(first.headers().firstValue("Location"), second.headers().firstValue("Location"));
        assertEquals(1, jdbc.queryForObject("select count(*) from order_requests", Integer.class));
    }
    @Test void keyContentMismatchIs409() throws Exception {
        String body = prepare(), key = UUID.randomUUID().toString();
        assertEquals(201, request("POST", "/api/orders", body, "Idempotency-Key", key).statusCode());
        error(request("POST", "/api/orders", body.replace("55000", "55001"), "Idempotency-Key", key), 409, "IDEMPOTENCY_KEY_REUSED");
    }
    @Test void cartChangeRequiresARevisionAndRejectsAStaleOne() throws Exception {
        String add = "{\"productCode\":\"P001\",\"quantity\":1}";
        error(request("POST", "/api/cart/items", add, null, null), 400, "INVALID_CART_REVISION");
        error(request("POST", "/api/cart/items", add, "X-Cart-Revision", "-1"), 400, "INVALID_CART_REVISION");
        error(request("POST", "/api/cart/items", add, "X-Cart-Revision", "bad"), 400, "HTTP_400");
        assertEquals(200, request("POST", "/api/cart/items", add, "X-Cart-Revision", "0").statusCode());
        error(request("PATCH", "/api/cart/items/P001", "{\"quantity\":2}", "X-Cart-Revision", "0"), 409, "CART_CHANGED");
        assertEquals(1, jdbc.queryForObject("select quantity from cart_items", Integer.class));
    }
    @Test void deleteUsesRevisionHeaderWithoutARequestBody() throws Exception {
        prepare();
        error(request("DELETE", "/api/cart/items/P001", null, null, null), 400, "INVALID_CART_REVISION");
        var removed = request("DELETE", "/api/cart/items/P001", null, "X-Cart-Revision", "1");
        assertEquals(200, removed.statusCode()); assertEquals(2, json.readTree(removed.body()).get("revision").asLong());
        assertEquals(0, json.readTree(removed.body()).get("totalQuantity").asInt());
    }
}
