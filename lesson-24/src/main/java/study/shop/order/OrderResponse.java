package study.shop.order;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record OrderResponse(
        long id, OrderStatus status, String paymentMode, Instant createdAt,
        List<Item> items, long subtotal, long discount, long shippingFee, long total) {
    public OrderResponse {
        items = List.copyOf(items);
    }

    public static OrderResponse from(ShopOrder order) {
        List<Item> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductCode))
                .map(item -> new Item(item.getProductCode(), item.getProductName(),
                        item.getUnitPrice(), item.getQuantity(), item.lineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getPaymentMode(),
                order.getCreatedAt(), items, order.getSubtotal(), order.getDiscount(),
                order.getShippingFee(), order.getTotal());
    }

    public record Item(String productCode, String productName,
                       long unitPrice, int quantity, long lineTotal) {
    }
}
