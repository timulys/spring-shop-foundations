package study.catalog;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogHttpTest {
    @LocalServerPort int port;
    private final JsonMapper json = JsonMapper.builder().build();

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (body == null) {
                request.GET();
            } else {
                request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private JsonNode lookup(String body, int status) throws Exception {
        var response = send("POST", "/internal/products/lookup", body);
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("X-Request-Id").isPresent());
        var result = json.readTree(response.body());
        if (status != 200) {
            assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(),
                    result.get("requestId").asText());
        }
        return result;
    }

    @Test
    void listsAndFiltersProductsWithTheExistingPublicContract() throws Exception {
        var all = send("GET", "/api/products", null);
        assertEquals(200, all.statusCode());
        assertEquals(3, json.readTree(all.body()).size());
        String category = URLEncoder.encode("주변기기", StandardCharsets.UTF_8);
        var result = send("GET", "/api/products?category=" + category + "&maxPrice=15000", null);
        assertEquals(200, result.statusCode());
        var products = json.readTree(result.body());
        assertEquals(1, products.size());
        assertEquals("P002", products.get(0).get("code").asText());
        assertEquals(404, send("GET", "/api/products/NO_SUCH_PRODUCT", null).statusCode());
        assertEquals(400, send("GET", "/api/products?maxPrice=-1", null).statusCode());
        assertEquals(400, send("GET", "/api/products?q=" + "a".repeat(41), null).statusCode());
    }

    @Test
    void lookupNormalizesCodesAndReturnsOnlyRequestedFieldsInCodeOrder() throws Exception {
        var result = lookup("{\"productCodes\":[\"P002\",\" P001 \"]}", 200);
        assertEquals(1, result.size());
        var products = result.get("products");
        assertEquals(2, products.size());
        assertEquals("P001", products.get(0).get("code").asText());
        assertEquals("키보드", products.get(0).get("name").asText());
        assertEquals(30000, products.get(0).get("unitPrice").asLong());
        assertEquals(3, products.get(0).size());
        assertEquals("P002", products.get(1).get("code").asText());
    }

    @Test
    void missingProductsProducePartialOrEmptySuccessfulResults() throws Exception {
        var partial = lookup("{\"productCodes\":[\"NO_SUCH_PRODUCT\",\"P001\"]}", 200);
        assertEquals(1, partial.get("products").size());
        var empty = lookup("{\"productCodes\":[\"NO_SUCH_PRODUCT\"]}", 200);
        assertEquals(0, empty.get("products").size());
    }

    @Test
    void allowsOneHundredCodesAndTwentyUtf16Units() throws Exception {
        List<String> hundred = IntStream.range(0, 100).mapToObj(i -> "code_" + i).toList();
        assertEquals(0, lookup(json.writeValueAsString(java.util.Map.of("productCodes", hundred)), 200)
                .get("products").size());
        String supplementary = "😀".repeat(10);
        assertEquals(20, supplementary.length());
        assertEquals(0, lookup(json.writeValueAsString(java.util.Map.of("productCodes", List.of(supplementary))), 200)
                .get("products").size());
        assertEquals(0, lookup("{\"productCodes\":[\"lower_case-code\"]}", 200).get("products").size());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("{}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":null}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[null]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[\" \"]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[\"P001\",\" P001 \"]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[\"" + "a".repeat(21) + "\"]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[\"" + "😀".repeat(11) + "\"]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":[" + IntStream.range(0, 101)
                        .mapToObj(i -> "\"P" + i + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}", "INVALID_PRODUCT_CODES"),
                Arguments.of("{\"productCodes\":\"P001\"}", "INVALID_REQUEST"),
                Arguments.of("{\"productCodes\":[123]}", "INVALID_REQUEST"),
                Arguments.of("{\"productCodes\":[true]}", "INVALID_REQUEST"),
                Arguments.of("{\"productCodes\":[{}]}", "INVALID_REQUEST"),
                Arguments.of("[]", "INVALID_REQUEST"),
                Arguments.of("null", "INVALID_REQUEST"),
                Arguments.of("{", "INVALID_REQUEST"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsMalformedOrAmbiguousLookupRequests(String body, String expectedCode) throws Exception {
        assertEquals(expectedCode, lookup(body, 400).get("code").asText());
    }

    @Test
    void exposesOnlyStatusInInternalHealthResponse() throws Exception {
        var response = send("GET", "/actuator/health/readiness", null);
        assertEquals(200, response.statusCode());
        assertEquals(json.readTree("{\"status\":\"UP\"}"), json.readTree(response.body()));
    }
}
