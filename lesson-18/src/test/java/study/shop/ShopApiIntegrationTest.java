package study.shop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(HttpFailureTestConfiguration.class)
@TestMethodOrder(MethodOrderer.Random.class)
@Execution(ExecutionMode.SAME_THREAD)
class ShopApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private boolean testDatabaseVerified;

    @BeforeEach
    void prepareIndependentData() {
        assertEquals("shop_test", jdbc.queryForObject("select current_database()", String.class),
                "통합 테스트는 shop_test 전용 DB에서 실행합니다.");
        testDatabaseVerified = true;
        clearTestRows();
        System.out.println("HTTP 테스트 준비: 실제 서버 포트 " + port + ", DB shop_test");
    }

    @AfterEach
    void cleanUp() {
        try {
            if (testDatabaseVerified) clearTestRows();
        } finally {
            client.close();
        }
    }

    private void clearTestRows() {
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        jdbc.update("delete from cart_items");
        jdbc.update("update products set unit_price_won=30000 where code='P001'");
    }

    @Test
    void placesOrderThroughHttpAndChecksCommittedRows() throws Exception {
        add("P001", 2);
        add("P002", 1);
        Reply response = request("POST", "/api/orders", null);
        assertEquals(201, response.status());
        long id = response.body().get("id").asLong();
        assertEquals("/api/orders/" + id, response.headers().firstValue("Location").orElseThrow());
        assertEquals(70_000, response.body().get("total").asLong());
        assertEquals(70_000L, jdbc.queryForObject(
                "select total_won from orders where id=?", Long.class, id));
        assertEquals(2, count("order_items"));
        assertEquals(0, count("cart_items"));
        assertEquals(response.body(), request("GET", "/api/orders/" + id, null).body());
        System.out.println("201 생성: 응답과 DB 총액 70000, 주문 항목 2, 장바구니 0");
    }

    @ParameterizedTest
    @CsvSource({"49999, 0, 3000, 52999", "50000, 5000, 0, 45000"})
    void checksDiscountBoundaryThroughHttp(long price, long discount,
                                           long shipping, long total) throws Exception {
        jdbc.update("update products set unit_price_won=? where code='P001'", price);
        add("P001", 1);
        Reply response = request("POST", "/api/orders", null);
        assertEquals(201, response.status());
        assertEquals(discount, response.body().get("discount").asLong());
        assertEquals(shipping, response.body().get("shippingFee").asLong());
        assertEquals(total, response.body().get("total").asLong());
        assertEquals(total, jdbc.queryForObject("select total_won from orders", Long.class));
        System.out.printf("경계 소계 %d: 할인 %d, 배송비 %d, 총액 %d%n",
                price, discount, shipping, total);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void rejectsQuantityOutsideRangeWithoutSaving(int quantity) throws Exception {
        Reply response = request("POST", "/api/cart/items",
                "{\"productCode\":\"P001\",\"quantity\":" + quantity + "}");
        assertError(response, 400, "VALIDATION_ERROR");
        assertEquals("quantity", response.body().get("fieldErrors").get(0).get("field").asText());
        assertEquals(0, count("cart_items"));
        System.out.println("수량 " + quantity + ": 400 VALIDATION_ERROR, 저장 0건");
    }

    @Test
    void rejectsFractionalQuantityAsInvalidJson() throws Exception {
        Reply response = request("POST", "/api/cart/items",
                "{\"productCode\":\"P001\",\"quantity\":1.5}");
        assertError(response, 400, "INVALID_JSON");
        assertEquals(0, count("cart_items"));
    }

    @Test
    void rejectsMissingProductWithoutSaving() throws Exception {
        Reply response = request("POST", "/api/cart/items",
                "{\"productCode\":\"MISSING\",\"quantity\":1}");
        assertError(response, 404, "PRODUCT_NOT_FOUND");
        assertEquals(0, count("cart_items"));
    }

    @Test
    void rejectsEmptyCartWithoutCreatingOrder() throws Exception {
        assertError(request("POST", "/api/orders", null), 400, "EMPTY_CART");
        assertEquals(0, count("orders"));
        assertEquals(0, count("order_items"));
    }

    @Test
    void keepsCancelledStateAfterRepeatedCancel() throws Exception {
        add("P001", 2);
        long id = request("POST", "/api/orders", null).body().get("id").asLong();
        assertEquals(200, request("POST", "/api/orders/" + id + "/cancel", null).status());
        assertError(request("POST", "/api/orders/" + id + "/cancel", null),
                409, "ORDER_STATE_CONFLICT");
        assertEquals("CANCELLED", jdbc.queryForObject(
                "select status from orders where id=?", String.class, id));
        assertEquals(1, count("orders"));
        System.out.println("취소 200 → 재취소 409, DB 상태 CANCELLED 유지");
    }

    @Test
    void rollsBackServerTransactionEvenAfterFlush() throws Exception {
        add("P001", 2);
        add("P002", 1);
        // 제공된 테스트 전용 경로입니다. 일반 앱에는 이 경로가 없습니다.
        Reply response = request("POST", "/test/orders/fail-after-flush", null);
        assertError(response, 500, "INTERNAL_ERROR");
        assertEquals(0, count("orders"));
        assertEquals(0, count("order_items"));
        assertEquals(2, count("cart_items"));
        assertEquals(2, jdbc.queryForObject(
                "select quantity from cart_items where product_code='P001'", Integer.class));
        System.out.println("HTTP 실패 500: 주문 0, 주문 항목 0, 기존 장바구니 2종 유지");
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private void add(String code, int quantity) throws Exception {
        assertEquals(200, request("POST", "/api/cart/items",
                "{\"productCode\":\"" + code + "\",\"quantity\":" + quantity + "}").status());
    }

    private void assertError(Reply reply, int status, String code) {
        assertEquals(status, reply.status());
        assertEquals(code, reply.body().get("code").asText());
        assertFalse(reply.body().get("requestId").asText().isBlank());
        assertEquals(reply.headers().firstValue("X-Request-Id").orElseThrow(),
                reply.body().get("requestId").asText());
    }

    // 제공 도구: JDK HTTP 클라이언트로 실제 서버에 요청하고 JSON을 읽습니다.
    private Reply request(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), json.readTree(response.body()), response.headers());
    }

    private record Reply(int status, JsonNode body, java.net.http.HttpHeaders headers) {}
}
