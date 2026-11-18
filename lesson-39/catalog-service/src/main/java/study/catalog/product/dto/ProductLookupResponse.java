package study.catalog.product.dto;

import java.util.List;
import study.catalog.product.domain.Product;

public record ProductLookupResponse(List<Item> products) {
    public ProductLookupResponse {
        products = List.copyOf(products);
    }

    public record Item(String code, String name, long unitPrice) {
        public static Item from(Product product) {
            return new Item(product.getCode(), product.getName(), product.getUnitPrice());
        }
    }
}
