package study.shop;

import java.util.Comparator;
import java.util.List;

public record OrderPreviewResponse(
        long cartRevision, String paymentMode, List<OrderResponse.Item> items,
        long subtotal, long discount, long shippingFee, long total) {
    public OrderPreviewResponse {
        items = List.copyOf(items);
    }

    public static OrderPreviewResponse from(Cart cart, ShopOrder order) {
        List<OrderResponse.Item> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductCode))
                .map(item -> new OrderResponse.Item(item.getProductCode(),
                        item.getProductName(), item.getUnitPrice(),
                        item.getQuantity(), item.lineTotal()))
                .toList();
        return new OrderPreviewResponse(cart.getRevision(), order.getPaymentMode(),
                items, order.getSubtotal(), order.getDiscount(),
                order.getShippingFee(), order.getTotal());
    }
}
