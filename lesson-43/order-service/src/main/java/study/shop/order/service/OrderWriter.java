package study.shop.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.shop.cart.domain.Cart;
import study.shop.cart.repository.CartRepository;
import study.shop.cart.service.CartStore;
import study.shop.order.domain.ShopOrder;
import study.shop.order.dto.OrderResponse;
import study.shop.order.repository.OrderRepository;

@Service
public class OrderWriter {
    private final CartRepository carts;
    private final OrderRepository orders;
    private final study.shop.order.repository.OrderRequestRepository requests;
    public OrderWriter(CartRepository carts, OrderRepository orders, study.shop.order.repository.OrderRequestRepository requests) {
        this.carts = carts;
        this.orders = orders;
        this.requests = requests;
    }

    @Transactional
    public OrderResponse save(long memberId, long expectedRevision, ShopOrder order, OrderIntent intent) {
        Cart cart = carts.findWithItems(memberId).orElseThrow();
        CartStore.requireRevision(cart.getRevision(), expectedRevision);
        if (order.getMemberId() != memberId) throw new IllegalArgumentException("주문 회원이 다릅니다.");
        orders.save(order);
        cart.clear();
        requests.save(new study.shop.order.domain.OrderRequestRecord(
                memberId, intent.key(), intent.hash(), order.getId()));
        carts.flush();
        return OrderResponse.from(order);
    }
}
