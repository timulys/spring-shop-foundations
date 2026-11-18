package study.shop.order.infrastructure;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import study.shop.common.ApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CatalogHttpClient {
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private final RestClient client;
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    public CatalogHttpClient(RestClient client) { this.client = client; }

    public JsonNode lookup(List<String> codes) {
        return read(client.post().uri("/internal/products/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("productCodes", codes)));
    }

    private JsonNode read(RestClient.RequestHeadersSpec<?> request) {
        try {
            return request.exchange((outgoing, response) -> {
                if (response.getStatusCode().value() != 200) throw unavailable();
                MediaType type;
                try { type = response.getHeaders().getContentType(); }
                catch (IllegalArgumentException ex) { throw invalidResponse(); }
                if (type == null || !MediaType.APPLICATION_JSON.isCompatibleWith(type)) {
                    throw invalidResponse();
                }
                byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_RESPONSE_BYTES) throw invalidResponse();
                try {
                    JsonNode body = json.readTree(bytes);
                    if (body == null || body.isNull()) throw invalidResponse();
                    return body;
                } catch (ApiException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw invalidResponse();
                }
            });
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    public static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                "상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    public static ApiException invalidResponse() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_RESPONSE_INVALID",
                "상품 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
