package study.shop.order.infrastructure;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import study.shop.common.ApiException;
import study.shop.order.service.port.ProductLookup;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
class RequestTracePropagationTest extends study.shop.common.CatalogServerTestBase {
    @Autowired ProductLookup lookup;
    @Test void keepsTheIncomingRequestIdAcrossBothReadAttempts(org.springframework.boot.test.system.CapturedOutput output) {
        MDC.put("requestId", "trace-retry-0001");
        try {
            CATALOG.batchStatus=503;
            assertThrows(ApiException.class,()->lookup.requireAll(List.of("P001")));
            assertEquals(List.of("trace-retry-0001","trace-retry-0001"),CATALOG.requestIds);
            var json = tools.jackson.databind.json.JsonMapper.builder().build();
            var rows = output.getAll().lines().filter(line -> line.startsWith("{") && line.contains("catalog_lookup_failure"))
                    .map(json::readTree).toList();
            assertEquals(2, rows.size());
            assertTrue(rows.stream().allMatch(row -> row.get("requestId").asText().equals("trace-retry-0001")));
            assertFalse(output.getAll().contains("failed to append"));
        } finally {MDC.remove("requestId");}
    }
    @Test void directNonHttpCallsGetAnIdForOneWholeLookup() {
        assertNull(MDC.get("requestId")); CATALOG.batchStatus=503;
        assertThrows(ApiException.class,()->lookup.requireAll(List.of("P001")));
        assertEquals(2,CATALOG.requestIds.size());
        assertTrue(study.shop.common.RequestIds.valid(CATALOG.requestIds.getFirst()));
        assertEquals(CATALOG.requestIds.getFirst(),CATALOG.requestIds.getLast());
        assertNull(MDC.get("requestId"));
    }
}
