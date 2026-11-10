package study.shop.order;

final class OrderTestRequests {
    private OrderTestRequests() {}

    static PlaceOrderRequest current(OrderService service) {
        OrderPreviewResponse preview = service.preview();
        return new PlaceOrderRequest(preview.cartRevision(), preview.total());
    }
}
