package study.shop.common;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.*;
import java.time.Duration;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;

/** 실제 세션 쿠키와 CSRF 토큰으로 요청하는 테스트 준비 도구입니다. */
public final class CsrfTestClient {
    private CsrfTestClient() { }
    public static HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }
    public static HttpResponse<String> send(HttpClient client, HttpRequest request) throws Exception {
        return send(client, request, true);
    }
    public static HttpResponse<String> send(HttpClient client, HttpRequest request, boolean supplyCourseHeaders) throws Exception {
        if (supplyCourseHeaders && request.method().equals("POST") && request.uri().getPath().equals("/api/orders")
                && request.headers().firstValue("Idempotency-Key").isEmpty()) {
            request = HttpRequest.newBuilder(request, (name,value) -> true)
                    .header("Idempotency-Key", java.util.UUID.randomUUID().toString()).build();
        }
        if (supplyCourseHeaders && Set.of("POST","PATCH","DELETE").contains(request.method())
                && request.uri().getPath().startsWith("/api/cart/items")
                && request.headers().firstValue("X-Cart-Revision").isEmpty()) {
            var cartReply = client.send(HttpRequest.newBuilder(request.uri().resolve("/api/cart"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (cartReply.statusCode() == 200) {
                var cart = JsonMapper.builder().build().readTree(cartReply.body());
                request = HttpRequest.newBuilder(request, (name,value) -> true)
                        .header("X-Cart-Revision", cart.get("revision").asText()).build();
            }
        }
        if (!Set.of("GET","HEAD","OPTIONS","TRACE").contains(request.method())) {
            var reply=client.send(HttpRequest.newBuilder(request.uri().resolve("/api/csrf"))
                    .timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(reply.statusCode()!=200) throw new IllegalStateException("CSRF setup failed: "+reply.statusCode());
            var token=JsonMapper.builder().build().readTree(reply.body());
            request=HttpRequest.newBuilder(request,(name,value)->true)
                    .header(token.get("headerName").asText(),token.get("token").asText()).build();
        }
        return client.send(request,HttpResponse.BodyHandlers.ofString());
    }
}
