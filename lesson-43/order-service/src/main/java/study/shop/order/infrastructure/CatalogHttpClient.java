package study.shop.order.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import study.shop.common.ApiException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CatalogHttpClient {
    private static final Logger log = LoggerFactory.getLogger(CatalogHttpClient.class);
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private final HttpClient client;
    private final CatalogCallPolicy policy;
    private final String baseUrl;
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    public CatalogHttpClient(HttpClient client, CatalogCallPolicy policy,
                             @Value("${catalog.base-url}") String baseUrl) {
        this.client = client;
        this.policy = policy;
        this.baseUrl = baseUrl;
    }

    public JsonNode lookup(List<String> codes) {
        long deadline = System.nanoTime() + policy.totalTimeoutMillis() * 1_000_000L;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            if (Thread.currentThread().isInterrupted()) throw unavailable();
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;
            long timeout = Math.min(remaining, policy.attemptTimeoutMillis() * 1_000_000L);
            try {
                return readOnce(codes, Duration.ofNanos(timeout), deadline);
            } catch (RetryableFailure ex) {
                log.warn("catalog lookup attempt={} failed; remainingMs={}", attempt,
                        Math.max(0, (deadline - System.nanoTime()) / 1_000_000));
                if (attempt == policy.maxAttempts()) break;
                long delay = policy.retryDelayMillis() * 1_000_000L;
                if (deadline - System.nanoTime() <= delay) break;
                try {
                    Thread.sleep(Duration.ofNanos(delay));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw unavailable();
                }
            }
        }
        throw unavailable();
    }

    private JsonNode readOnce(List<String> codes, Duration timeout, long deadline) {
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(timeout);
        RestClient rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        try {
            return rest.post().uri("/internal/products/lookup")
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("productCodes", codes))
                    .exchange((outgoing, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 502 || status == 503 || status == 504) throw new RetryableFailure();
                        if (status != 200) throw unavailable();
                        MediaType type;
                        try { type = response.getHeaders().getContentType(); }
                        catch (IllegalArgumentException ex) { throw invalidResponse(); }
                        if (type == null || !MediaType.APPLICATION_JSON.isCompatibleWith(type)) throw invalidResponse();
                        byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                        if (deadline - System.nanoTime() <= 0) throw new RetryableFailure();
                        if (bytes.length == 0 || bytes.length > MAX_RESPONSE_BYTES) throw invalidResponse();
                        JsonNode body;
                        try { body = json.readTree(bytes); }
                        catch (RuntimeException ex) { throw invalidResponse(); }
                        if (deadline - System.nanoTime() <= 0) throw new RetryableFailure();
                        if (body == null || body.isNull()) throw invalidResponse();
                        return body;
                    });
        } catch (RestClientException ex) {
            throw new RetryableFailure();
        }
    }

    private static final class RetryableFailure extends RuntimeException {}

    public static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                "상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    public static ApiException invalidResponse() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_RESPONSE_INVALID",
                "상품 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
