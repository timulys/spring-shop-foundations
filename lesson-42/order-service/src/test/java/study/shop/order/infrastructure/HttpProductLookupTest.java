package study.shop.order.infrastructure;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import study.shop.common.ApiException;
import study.shop.order.service.port.ProductLookup;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class HttpProductLookupTest extends study.shop.common.CatalogServerTestBase {
    @Autowired ProductLookup lookup;
    @Autowired PlatformTransactionManager manager;

    @Test
    void makesOneBatchRequestAndReturnsImmutableSnapshots() {
        var result = lookup.requireAll(List.of("P002", "P001"));
        assertEquals(2, result.size());
        assertEquals(30000, result.get("P001").unitPrice().won());
        assertEquals(1, CATALOG.batchCalls.get());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void missingProductIsDifferentFromRemoteFailure() {
        var values = lookup.findByCodes(List.of("P001", "MISSING"));
        assertEquals(1, values.size());
        ApiException error = assertThrows(ApiException.class,
                () -> lookup.requireAll(List.of("P001", "MISSING")));
        assertEquals(404, error.status().value());
        assertEquals("PRODUCT_NOT_FOUND", error.code());
        CATALOG.failAll = true;
        error = assertThrows(ApiException.class, () -> lookup.findByCodes(List.of("P001")));
        assertEquals(503, error.status().value());
        assertEquals("CATALOG_UNAVAILABLE", error.code());
    }

    @Test
    void emptyCartNeedsNoRemoteRequest() {
        assertTrue(lookup.findByCodes(List.of()).isEmpty());
        assertEquals(0, CATALOG.calls.size());
    }

    @Test
    void rejectsRemoteLookupInsideAnActiveDatabaseTransaction() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager)
                .execute(status -> lookup.findByCodes(List.of("P001"))));
        assertEquals(0, CATALOG.calls.size());
    }

    static Stream<String> invalidResponses() {
        String valid = "{\"code\":\"P001\",\"name\":\"키보드\",\"unitPrice\":30000}";
        return Stream.of("{", "null", "[]", "{}", "{\"products\":null}", "{\"products\":{}}",
                "{\"products\":[null]}",
                "{\"products\":[" + valid.replace("P001", "UNEXPECTED") + "]}",
                "{\"products\":[" + valid + "," + valid + "]}",
                "{\"products\":[" + valid.replace("30000", "\"30000\"") + "]}",
                "{\"products\":[" + valid.replace("30000", "30000.0") + "]}",
                "{\"products\":[" + valid.replace("30000", "0") + "]}",
                "{\"products\":[" + valid.replace("30000", "1000001") + "]}",
                "{\"products\":[" + valid.replace("30000", "9223372036854775808") + "]}",
                "{\"products\":[" + valid.replace("P001", " P001 ") + "]}",
                "{\"products\":[" + valid.replace("키보드", " ") + "]}",
                "{\"products\":[" + valid.replace("키보드", "가".repeat(101)) + "]}",
                "{\"products\":[{\"code\":\"P001\",\"unitPrice\":30000}]}",
                "{\"products\":[" + valid + "]} {}",
                "{\"products\":[],\"products\":[" + valid + "]}");
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void rejectsMalformedOrUntrustedProductValues(String body) {
        CATALOG.forcedBatchBody = body;
        var error = assertThrows(ApiException.class, () -> lookup.findByCodes(List.of("P001")));
        assertEquals(503, error.status().value());
        assertEquals("CATALOG_RESPONSE_INVALID", error.code());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"text/plain", "not a media type", "application/problem+json"})
    void rejectsUnexpectedOrMalformedContentType(String type) {
        CATALOG.contentType = type;
        var error = assertThrows(ApiException.class, () -> lookup.findByCodes(List.of("P001")));
        assertEquals("CATALOG_RESPONSE_INVALID", error.code());
    }

    @Test
    void rejectsAnOversizedResponseBeforeParsingIt() {
        CATALOG.forcedBatchBody = " ".repeat(1_048_577);
        var error = assertThrows(ApiException.class, () -> lookup.findByCodes(List.of("P001")));
        assertEquals("CATALOG_RESPONSE_INVALID", error.code());
    }
}
