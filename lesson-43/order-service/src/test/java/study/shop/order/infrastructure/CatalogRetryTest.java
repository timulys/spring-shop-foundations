package study.shop.order.infrastructure;

import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import study.shop.common.ApiException;
import study.shop.order.service.port.ProductLookup;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CatalogRetryTest extends study.shop.common.CatalogServerTestBase {
    @Autowired ProductLookup lookup;
    @Autowired CatalogCallPolicy policy;
    @Autowired HttpClient client;

    @Test
    void recoversOnSecondReadAfterOneTransient503() {
        CATALOG.beforeBatch = () -> CATALOG.batchStatus = CATALOG.batchCalls.get() == 1 ? 503 : 200;
        long started = System.nanoTime();
        var products = lookup.requireAll(List.of("P001", "P002"));
        long elapsed = elapsedMillis(started);
        assertEquals(2, products.size());
        assertEquals(2, CATALOG.batchCalls.get());
        assertTrue(elapsed >= policy.retryDelayMillis() - 10, "retry backoff was skipped");
        assertTrue(elapsed < policy.totalTimeoutMillis() + 1000, "elapsed=" + elapsed);
        System.out.println("503 뒤 두 번째 조회 성공: attempts=2, elapsedMs=" + elapsed);
    }

    @ParameterizedTest
    @ValueSource(ints = {502, 503, 504})
    void stopsAtTwoAttemptsForRetryableStatus(int status) {
        CATALOG.batchStatus = status;
        var error = assertThrows(ApiException.class, () -> lookup.requireAll(List.of("P001")));
        assertEquals(503, error.status().value());
        assertEquals("CATALOG_UNAVAILABLE", error.code());
        assertEquals(2, CATALOG.batchCalls.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500})
    void doesNotRetryStatusesOutsideTheReadRetryPolicy(int status) {
        CATALOG.batchStatus = status;
        var error = assertThrows(ApiException.class, () -> lookup.requireAll(List.of("P001")));
        assertEquals("CATALOG_UNAVAILABLE", error.code());
        assertEquals(1, CATALOG.batchCalls.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{}"})
    void invalidJsonOrProtocolIsNotRetried(String body) {
        CATALOG.forcedBatchBody = body;
        var error = assertThrows(ApiException.class, () -> lookup.requireAll(List.of("P001")));
        assertEquals("CATALOG_RESPONSE_INVALID", error.code());
        assertEquals(1, CATALOG.batchCalls.get());
    }

    @Test
    void aMissingProductInASuccessfulResponseIsNotRetried() {
        var error = assertThrows(ApiException.class, () -> lookup.requireAll(List.of("P001", "MISSING")));
        assertEquals(404, error.status().value());
        assertEquals(1, CATALOG.batchCalls.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void totalBudgetIncludesBothAttemptsBackoffAndResponseBody(boolean delayAfterHeaders) {
        var release = new CountDownLatch(1);
        Runnable delay = () -> {
            try { release.await(8, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        };
        if (delayAfterHeaders) CATALOG.beforeBody = delay; else CATALOG.beforeBatch = delay;
        long started = System.nanoTime();
        try {
            var error = assertThrows(ApiException.class, () -> lookup.requireAll(List.of("P001")));
            long elapsed = elapsedMillis(started);
            assertEquals("CATALOG_UNAVAILABLE", error.code());
            assertEquals(2, CATALOG.batchCalls.get());
            assertTrue(elapsed >= policy.totalTimeoutMillis() - 150, "elapsed=" + elapsed);
            assertTrue(elapsed <= policy.totalTimeoutMillis() + 1000, "elapsed=" + elapsed);
            System.out.println("전체 상품 조회 제한: afterHeaders=" + delayAfterHeaders
                    + ", attempts=2, budgetMs=" + policy.totalTimeoutMillis() + ", elapsedMs=" + elapsed);
        } finally {
            release.countDown();
            CATALOG.beforeBatch = null;
            CATALOG.beforeBody = null;
        }
    }

    @Test
    void connectionRefusalEndsWithinTheWholeCallBudget() throws Exception {
        int unusedPort;
        try (var socket = new ServerSocket(0)) { unusedPort = socket.getLocalPort(); }
        var peer = new CatalogHttpClient(client, policy, "http://127.0.0.1:" + unusedPort);
        long started = System.nanoTime();
        var error = assertThrows(ApiException.class, () -> peer.lookup(List.of("P001")));
        long elapsed = elapsedMillis(started);
        assertEquals("CATALOG_UNAVAILABLE", error.code());
        assertTrue(elapsed <= policy.totalTimeoutMillis() + 1000, "elapsed=" + elapsed);
    }

    @Test
    void rejectsUnboundedOrInvalidRetrySettings() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogCallPolicy(0, 1500, 2500, 100, 2));
        assertThrows(IllegalArgumentException.class, () -> new CatalogCallPolicy(500, 0, 2500, 100, 2));
        assertThrows(IllegalArgumentException.class, () -> new CatalogCallPolicy(500, 1500, 0, 100, 2));
        assertThrows(IllegalArgumentException.class, () -> new CatalogCallPolicy(500, 1500, 2500, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> new CatalogCallPolicy(500, 1500, 2500, 100, 4));
    }

    private static long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }
}
