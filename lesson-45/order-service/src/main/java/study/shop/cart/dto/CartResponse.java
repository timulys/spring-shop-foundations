package study.shop.cart.dto;

import java.util.List;

public record CartResponse(List<Item> items, int totalQuantity, long subtotal, long revision) {
    public CartResponse { items = List.copyOf(items); }

    public record Item(String productCode, String name, long unitPrice,
                       int quantity, long lineTotal) { }
}
