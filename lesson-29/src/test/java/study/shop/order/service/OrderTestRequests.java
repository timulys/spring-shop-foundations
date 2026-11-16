package study.shop.order.service;

import study.shop.order.dto.OrderPreviewResponse;
import study.shop.order.dto.PlaceOrderRequest;

final class OrderTestRequests {
    private OrderTestRequests() {}

    static PlaceOrderRequest current(OrderService service) {
        OrderPreviewResponse preview = service.preview();
        return new PlaceOrderRequest(preview.cartRevision(), preview.total());
    }
}
