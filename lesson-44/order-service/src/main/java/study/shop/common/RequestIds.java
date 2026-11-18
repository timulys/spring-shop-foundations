package study.shop.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class RequestIds {
    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{8,64}");
    private RequestIds() {}
    public static boolean valid(String value) { return value != null && VALID.matcher(value).matches(); }
    public static String incoming(Enumeration<String> values) {
        var headers = Collections.list(values);
        return headers.size() == 1 && valid(headers.getFirst()) ? headers.getFirst() : UUID.randomUUID().toString();
    }
    public static String currentOrNew() {
        String value = MDC.get(MDC_KEY);
        return valid(value) ? value : UUID.randomUUID().toString();
    }
}
