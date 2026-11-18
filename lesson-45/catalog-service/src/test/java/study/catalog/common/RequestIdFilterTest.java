package study.catalog.common;

import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import static org.junit.jupiter.api.Assertions.*;

class RequestIdFilterTest {
    @Test void propagatesValidIdAndRestoresThePreviousThreadContext() throws Exception {
        MDC.put("requestId", "previous-thread-id");
        try {
            var request = new MockHttpServletRequest("GET", "/api/orders/1");
            request.addHeader("X-Request-Id", "trace-valid-0001");
            var response = new MockHttpServletResponse();
            new RequestIdFilter().doFilter(request, response, (incoming, outgoing) -> {
                assertEquals("trace-valid-0001", MDC.get("requestId"));
                assertEquals("trace-valid-0001", incoming.getAttribute(RequestIdFilter.ATTRIBUTE));
            });
            assertEquals("trace-valid-0001", response.getHeader("X-Request-Id"));
            assertEquals("previous-thread-id", MDC.get("requestId"));
        } finally { MDC.remove("requestId"); }
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={"short", "bad value", "quoted\"value", "line\nfeed", "한글요청식별자"})
    void replacesMissingOrUnsafeId(String value) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        if (value != null) request.addHeader("X-Request-Id", value);
        var response = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(request,response,(incoming,outgoing)->{});
        assertTrue(RequestIds.valid(response.getHeader("X-Request-Id")));
        assertNotEquals(value,response.getHeader("X-Request-Id")); assertNull(MDC.get("requestId"));
    }
    @Test void multipleAndOverlongHeadersAreReplaced() throws Exception {
        for (boolean duplicate : new boolean[]{false,true}) {
            var request = new MockHttpServletRequest("GET", "/api/products");
            String value = duplicate ? "trace-valid-0001" : "a".repeat(65);
            request.addHeader("X-Request-Id", value);
            if (duplicate) request.addHeader("X-Request-Id", "trace-valid-0002");
            var response = new MockHttpServletResponse();
            new RequestIdFilter().doFilter(request,response,(incoming,outgoing)->{});
            assertTrue(RequestIds.valid(response.getHeader("X-Request-Id")));
            assertNotEquals(value,response.getHeader("X-Request-Id"));
        }
    }
    @Test void logsOnlyRouteMetadataAndCleansContextWhenTheChainThrows() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        var appender = new ListAppender<ILoggingEvent>() {
            @Override protected void append(ILoggingEvent event) { event.prepareForDeferredProcessing(); super.append(event); }
        };
        appender.start(); logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/secret-path-value");
            request.setQueryString("password=secret-query-value");
            request.addHeader("Authorization", "Bearer secret-auth-value");
            request.addHeader("Cookie", "JSESSIONID=secret-cookie-value");
            request.setContent("password=secret-body-value".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            request.addHeader("X-Request-Id", "trace-error-0001");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/orders/{id}");
            assertThrows(ServletException.class, () -> new RequestIdFilter().doFilter(request,
                    new MockHttpServletResponse(), (incoming,outgoing)->{throw new ServletException("failure");}));
            assertNull(MDC.get("requestId")); assertEquals(1,appender.list.size());
            var event = appender.list.getFirst();
            Map<String,Object> fields = event.getKeyValuePairs().stream().collect(Collectors.toMap(kv->kv.key,kv->kv.value));
            assertEquals(500,fields.get("status")); assertEquals("/api/orders/{id}",fields.get("route"));
            assertEquals("trace-error-0001",event.getMDCPropertyMap().get("requestId"));
            assertFalse((event.getFormattedMessage()+fields).contains("secret-"));
        } finally { logger.detachAppender(appender); appender.stop(); MDC.remove("requestId"); }
    }
}
