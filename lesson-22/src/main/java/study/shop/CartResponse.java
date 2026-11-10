package study.shop;

import java.util.List;

public record CartResponse(List<Item> items, int totalQuantity, long subtotal) {
    public CartResponse { items = List.copyOf(items); }

    public record Item(String productCode, String name, long unitPrice,
                       int quantity, long lineTotal) { }
}
