package study.shop.order.service;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import study.shop.common.ApiException;
import study.shop.order.dto.OrderResponse;
import study.shop.order.repository.OrderRepository;
import study.shop.order.repository.OrderRequestRepository;

@Service
public class OrderRequestReader {
    private final OrderRequestRepository requests;
    private final OrderRepository orders;
    public OrderRequestReader(OrderRequestRepository requests, OrderRepository orders) {
        this.requests = requests; this.orders = orders;
    }
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<OrderResponse> completed(long memberId, OrderIntent intent) {
        return requests.findByMemberIdAndRequestKey(memberId, intent.key()).map(record -> {
            if (!record.getRequestHash().equals(intent.hash())) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "같은 주문 요청 키에 다른 주문 내용이 들어왔습니다.");
            }
            return OrderResponse.from(orders.findDetailById(memberId, record.getOrderId()).orElseThrow());
        });
    }
}
