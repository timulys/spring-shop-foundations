package study.shop.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;

public final class CatalogStub {
    public record Product(String code, String name, long unitPrice, String category) {}
    public record Call(String method, String path, String body) {}
    public final Map<String, Product> products = new ConcurrentHashMap<>();
    public final List<Call> calls = new CopyOnWriteArrayList<>();
    public final AtomicInteger batchCalls = new AtomicInteger();
    public volatile Runnable beforeBatch;
    public volatile Runnable beforeBody;
    public volatile String forcedBatchBody;
    public volatile int batchStatus = 200;
    public volatile boolean failAll;
    public volatile String contentType = "application/json";
    private final HttpServer server;
    private final JsonMapper json = JsonMapper.builder().build();

    public CatalogStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool(r -> {
                var thread = new Thread(r, "test-catalog");
                thread.setDaemon(true);
                return thread;
            }));
            server.createContext("/", this::handle);
            server.start();
            reset();
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    public String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void reset() {
        beforeBatch = null;
        beforeBody = null;
        forcedBatchBody = null;
        batchStatus = 200;
        failAll = false;
        contentType = "application/json";
        products.clear();
        set("P001", "키보드", 30000, "주변기기");
        set("P002", "마우스", 15000, "주변기기");
        set("P003", "노트", 5000, "문구");
        calls.clear();
        batchCalls.set(0);
    }

    public void set(String code, String name, long price, String category) {
        products.put(code, new Product(code, name, price, category));
    }
    public void price(String code, long price) {
        var p = products.get(code);
        set(code, p.name(), price, p.category());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            calls.add(new Call(exchange.getRequestMethod(), path, body));
            if (failAll) { respond(exchange, 503, "{}"); return; }
            if (path.equals("/internal/products/lookup") && exchange.getRequestMethod().equals("POST")) {
                batchCalls.incrementAndGet();
                Runnable hook = beforeBatch;
                if (hook != null) hook.run();
                if (forcedBatchBody != null || batchStatus != 200) {
                    respond(exchange, batchStatus, forcedBatchBody == null ? "{}" : forcedBatchBody);
                    return;
                }
                var result = new TreeMap<String, Map<String, Object>>();
                for (var value : json.readTree(body).get("productCodes")) {
                    Product p = products.get(value.asText());
                    if (p != null) result.put(p.code(), Map.of("code", p.code(), "name", p.name(), "unitPrice", p.unitPrice()));
                }
                respond(exchange, 200, json.writeValueAsString(Map.of("products", new ArrayList<>(result.values()))));
                return;
            }
            if (path.equals("/api/products")) {
                Map<String,String> query = new TreeMap<>();
                String raw = exchange.getRequestURI().getRawQuery();
                if (raw != null) for (String part : raw.split("&")) {
                    String[] pair = part.split("=", 2);
                    query.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                            pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
                }
                String category = query.get("category"), q = query.get("q");
                Long max = query.containsKey("maxPrice") ? Long.valueOf(query.get("maxPrice")) : null;
                var result = new TreeMap<>(products).values().stream()
                        .filter(p -> category == null || category.isBlank() || p.category().equals(category.strip()))
                        .filter(p -> q == null || q.isBlank() || p.name().contains(q.strip()))
                        .filter(p -> max == null || p.unitPrice() <= max).toList();
                respond(exchange, 200, json.writeValueAsString(result));
                return;
            }
            if (path.startsWith("/api/products/")) {
                Product product = products.get(path.substring("/api/products/".length()));
                respond(exchange, product == null ? 404 : 200, product == null ? "{}" : json.writeValueAsString(product));
                return;
            }
            respond(exchange, 404, "{}");
        }
    }

    private void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        Runnable hook = beforeBody;
        if (hook != null) {
            exchange.getResponseBody().write(bytes, 0, 1);
            exchange.getResponseBody().flush();
            hook.run();
            exchange.getResponseBody().write(bytes, 1, bytes.length - 1);
        } else {
            exchange.getResponseBody().write(bytes);
        }
    }
}
