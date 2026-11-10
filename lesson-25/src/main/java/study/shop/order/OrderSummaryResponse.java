package study.shop.order;

import java.time.Instant;

// 목록에서는 항목 컬렉션에 접근하지 않고 요약 필드만 반환합니다.
public record OrderSummaryResponse(long id, OrderStatus status,
                                   long total, Instant createdAt) {
    public static OrderSummaryResponse from(ShopOrder order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus(),
                order.getTotal(), order.getCreatedAt());
    }
}
