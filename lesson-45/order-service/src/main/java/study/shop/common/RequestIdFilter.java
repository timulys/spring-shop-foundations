package study.shop.common;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = "shop.requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Set<String> FILTER_ROUTES = Set.of("/login", "/logout", "/api/session/me", "/api/csrf");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = RequestIds.incoming(request.getHeaders(RequestIds.HEADER));
        String previous = MDC.get(RequestIds.MDC_KEY);
        long started = System.nanoTime();
        boolean escapedFailure = false;
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(RequestIds.HEADER, requestId);
        MDC.put(RequestIds.MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException error) {
            escapedFailure = true; throw error;
        } finally {
            log.atInfo().addKeyValue("event", "http_request")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("route", route(request))
                    .addKeyValue("status", escapedFailure ? 500 : response.getStatus())
                    .addKeyValue("durationMs", (System.nanoTime() - started) / 1_000_000)
                    .log("HTTP request completed");
            if (previous == null) MDC.remove(RequestIds.MDC_KEY); else MDC.put(RequestIds.MDC_KEY, previous);
        }
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) return pattern.toString();
        return FILTER_ROUTES.contains(request.getRequestURI()) ? request.getRequestURI() : "unmatched";
    }
}
