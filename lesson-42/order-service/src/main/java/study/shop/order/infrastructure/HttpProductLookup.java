package study.shop.order.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;
import study.shop.order.service.port.ProductLookup;
import tools.jackson.databind.JsonNode;
import static study.shop.order.infrastructure.CatalogHttpClient.invalidResponse;

@Component
public class HttpProductLookup implements ProductLookup {
    private final CatalogHttpClient client;
    public HttpProductLookup(CatalogHttpClient client) { this.client = client; }

    @Override
    public Map<String, ProductSnapshot> findByCodes(List<String> codes) {
        if (codes.isEmpty()) return Map.of();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("상품 HTTP 조회는 DB 트랜잭션 밖에서 실행해야 합니다.");
        }
        if (codes.size() > 100 || new HashSet<>(codes).size() != codes.size()) {
            throw new IllegalArgumentException("중복 없이 상품 코드를 100개까지 조회할 수 있습니다.");
        }
        JsonNode response = client.lookup(codes);
        if (!response.isObject() || response.get("products") == null
                || !response.get("products").isArray()) throw invalidResponse();
        JsonNode values = response.get("products");
        if (values.size() > codes.size()) throw invalidResponse();
        var result = new LinkedHashMap<String, ProductSnapshot>();
        for (JsonNode value : values) {
            ProductSnapshot product = decode(value);
            if (!codes.contains(product.code()) || result.putIfAbsent(product.code(), product) != null) {
                throw invalidResponse();
            }
        }
        return Map.copyOf(result);
    }

    public static ProductSnapshot decode(JsonNode value) {
        if (value == null || !value.isObject()) throw invalidResponse();
        JsonNode code = value.get("code"), name = value.get("name"), price = value.get("unitPrice");
        if (code == null || !code.isTextual() || name == null || !name.isTextual()
                || price == null || !price.isIntegralNumber() || !price.canConvertToLong()) {
            throw invalidResponse();
        }
        String codeText = code.asText();
        if (!codeText.equals(codeText.strip())) throw invalidResponse();
        try {
            return new ProductSnapshot(codeText, name.asText(), Money.ofWon(price.asLong()));
        } catch (IllegalArgumentException ex) {
            throw invalidResponse();
        }
    }
}
