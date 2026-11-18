package study.shop.cart.service;

import java.util.List;
import study.shop.cart.domain.Cart;

public record CartSnapshot(long memberId, long revision, List<Item> items) {
    public CartSnapshot { items = List.copyOf(items); }
    public record Item(String code, int quantity) {}

    public static CartSnapshot from(Cart cart) {
        return new CartSnapshot(cart.getMemberId(), cart.getRevision(), cart.getItems().stream()
                .map(item -> new Item(item.getProductCode(), item.getQuantity())).toList());
    }

    public List<String> codes() { return items.stream().map(Item::code).toList(); }
}
