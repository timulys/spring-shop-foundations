package study.shop.catalog.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import study.shop.catalog.domain.Product;
import study.shop.order.infrastructure.CatalogHttpClient;
import study.shop.order.infrastructure.HttpProductLookup;
import tools.jackson.databind.JsonNode;

/** Keeps the browser API working until NGINX routing changes in lesson 41. */
@Service
public class ProductService {
    private final CatalogHttpClient catalog;
    public ProductService(CatalogHttpClient catalog) { this.catalog = catalog; }

    public List<Product> findAll(String category, String q, Long maxPrice) {
        JsonNode response = catalog.browse(category, q, maxPrice);
        if (!response.isArray()) throw CatalogHttpClient.invalidResponse();
        var result = new ArrayList<Product>();
        var seen = new HashSet<String>();
        for (JsonNode value : response) {
            Product product = decode(value);
            if (!seen.add(product.code())) throw CatalogHttpClient.invalidResponse();
            result.add(product);
        }
        return List.copyOf(result);
    }

    public Optional<Product> findByCode(String code) {
        JsonNode value = catalog.product(code);
        if (value == null) return Optional.empty();
        Product product = decode(value);
        if (!product.code().equals(code)) throw CatalogHttpClient.invalidResponse();
        return Optional.of(product);
    }

    private Product decode(JsonNode value) {
        var snapshot = HttpProductLookup.decode(value);
        JsonNode category = value.get("category");
        if (category == null || !category.isTextual() || category.asText().isBlank()
                || category.asText().length() > 40) throw CatalogHttpClient.invalidResponse();
        return new Product(snapshot.code(), snapshot.name(), snapshot.unitPrice().won(), category.asText());
    }
}
