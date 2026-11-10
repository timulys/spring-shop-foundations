package study.shop.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.Random.class)
class CheckoutHttpTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void prepare() throws Exception {
        assertEquals("shop_test", jdbc.queryForObject("select current_database()", String.class));
        clear();
        add("P001", 2);
        add("P002", 1);
    }

    @AfterEach
    void cleanup() {
        if ("shop_test".equals(jdbc.queryForObject("select current_database()", String.class))) clear();
        client.close();
    }

    private void clear() {
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        jdbc.update("delete from cart_items");
        jdbc.update("update products set unit_price_won=30000 where code='P001'");
    }

    @Test
    void previewCalculatesAllAmountsWithoutSavingAnOrder() throws Exception {
        Reply first = request("GET", "/api/orders/preview", null);
        Reply second = request("GET", "/api/orders/preview", null);
        assertEquals(200, first.status());
        assertEquals(first.body(), second.body());
        JsonNode b = first.body();
        assertEquals("SIMULATED", b.get("paymentMode").asText());
        assertEquals(75_000, b.get("subtotal").asLong());
        assertEquals(5_000, b.get("discount").asLong());
        assertEquals(0, b.get("shippingFee").asLong());
        assertEquals(70_000, b.get("total").asLong());
        assertEquals(2, b.get("items").size());
        assertEquals(0, count("orders"));
        assertEquals(0, count("order_items"));
        assertEquals(2, count("cart_items"));
    }

    @Test
    void acceptsConfirmedTotalAndClearsCartOnlyAfterSaving() throws Exception {
        JsonNode preview = request("GET", "/api/orders/preview", null).body();
        Reply order = request("POST", "/api/orders", confirmation(preview));
        assertEquals(201, order.status());
        assertEquals(70_000, order.body().get("total").asLong());
        assertEquals("CONFIRMED", order.body().get("status").asText());
        assertEquals("SIMULATED", order.body().get("paymentMode").asText());
        assertEquals(1, count("orders"));
        assertEquals(2, count("order_items"));
        assertEquals(0, count("cart_items"));
        assertEquals(70_000L, jdbc.queryForObject("select total_won from orders", Long.class));
    }

    @Test
    void priceChangeRequiresNewConfirmationAndLeavesCartIntact() throws Exception {
        JsonNode old = request("GET", "/api/orders/preview", null).body();
        jdbc.update("update products set unit_price_won=31000 where code='P001'");
        assertError(request("POST", "/api/orders", confirmation(old)), 409, "PRICE_CHANGED");
        assertEquals(0, count("orders"));
        assertEquals(2, count("cart_items"));
        JsonNode fresh = request("GET", "/api/orders/preview", null).body();
        assertEquals(72_000, fresh.get("total").asLong());
        assertEquals(201, request("POST", "/api/orders", confirmation(fresh)).status());
        assertEquals(72_000L, jdbc.queryForObject("select total_won from orders", Long.class));
    }

    @Test
    void cartChangeWithEqualTotalStillRequiresNewConfirmation() throws Exception {
        JsonNode old = request("GET", "/api/orders/preview", null).body();
        // Changing away and back keeps the final amount, but invalidates the old confirmation.
        assertEquals(200, request("PATCH", "/api/cart/items/P001", "{\"quantity\":1}").status());
        assertEquals(200, request("PATCH", "/api/cart/items/P001", "{\"quantity\":2}").status());
        assertError(request("POST", "/api/orders", confirmation(old)), 409, "CART_CHANGED");
        assertEquals(0, count("orders"));
        assertEquals(2, count("cart_items"));
    }

    @Test
    void clientCannotChooseTheSavedAmount() throws Exception {
        JsonNode preview = request("GET", "/api/orders/preview", null).body();
        String body = "{\"cartRevision\":" + preview.get("cartRevision").asLong()
                + ",\"expectedTotalWon\":1}";
        assertError(request("POST", "/api/orders", body), 409, "PRICE_CHANGED");
        assertEquals(0, count("orders"));
        assertEquals(2, count("cart_items"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"cartRevision\":0}",
            "{\"cartRevision\":-1,\"expectedTotalWon\":70000}",
            "{\"cartRevision\":0,\"expectedTotalWon\":-1}"})
    void requiredAndNonnegativeFields(String body) throws Exception {
        assertError(request("POST", "/api/orders", body), 400, "VALIDATION_ERROR");
        assertEquals(0, count("orders"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"cartRevision\":0,\"expectedTotalWon\":70000.5}",
            "{\"cartRevision\":0,\"expectedTotalWon\":\"70000\"}",
            "{\"cartRevision\":\"0\",\"expectedTotalWon\":70000}",
            "{\"cartRevision\":0.5,\"expectedTotalWon\":70000}"})
    void acceptsOnlyJsonIntegers(String body) throws Exception {
        assertError(request("POST", "/api/orders", body), 400, "INVALID_JSON");
        assertEquals(0, count("orders"));
    }

    @Test
    void missingBodyDoesNotBypassConfirmation() throws Exception {
        assertError(request("POST", "/api/orders", null), 400, "INVALID_JSON");
        assertEquals(0, count("orders"));
    }

    private String confirmation(JsonNode preview) {
        return "{\"cartRevision\":" + preview.get("cartRevision").asLong()
                + ",\"expectedTotalWon\":" + preview.get("total").asLong() + "}";
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private void add(String code, int quantity) throws Exception {
        assertEquals(200, request("POST", "/api/cart/items", "{\"productCode\":\""
                + code + "\",\"quantity\":" + quantity + "}").status());
    }

    private void assertError(Reply reply, int status, String code) {
        assertEquals(status, reply.status());
        assertEquals(code, reply.body().get("code").asText());
    }

    private Reply request(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()));
    }

    private record Reply(int status, JsonNode body) {}
}
