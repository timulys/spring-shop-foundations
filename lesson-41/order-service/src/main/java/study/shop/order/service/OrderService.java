package study.shop.order.service;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import study.shop.cart.service.CartSnapshot;
import study.shop.cart.service.CartStore;
import study.shop.common.ApiException;
import study.shop.order.domain.*;
import study.shop.order.dto.*;
import study.shop.order.repository.OrderRepository;
import study.shop.order.service.port.ProductLookup;

@Service
public class OrderService {
    private final CartStore carts;
    private final ProductLookup products;
    private final OrderRepository orders;
    private final DiscountPolicy discountPolicy;
    private final OrderWriter writer;

    public OrderService(CartStore carts, ProductLookup products, OrderRepository orders,
                        DiscountPolicy discountPolicy, OrderWriter writer) {
        this.carts = carts;
        this.products = products;
        this.orders = orders;
        this.discountPolicy = discountPolicy;
        this.writer = writer;
    }

    @Transactional(propagation = Propagation.NEVER)
    public OrderPreviewResponse preview(long memberId) {
        CartSnapshot cart = carts.read(memberId);
        requireItems(cart);
        ShopOrder order = calculate(cart, products.requireAll(cart.codes()));
        return OrderPreviewResponse.from(cart.revision(), order);
    }

    @Transactional(propagation = Propagation.NEVER)
    public OrderResponse place(long memberId, PlaceOrderRequest request) {
        CartSnapshot cart = carts.read(memberId);
        requireItems(cart);
        CartStore.requireRevision(cart.revision(), request.cartRevision());
        ShopOrder order = calculate(cart, products.requireAll(cart.codes()));
        if (order.getTotal() != request.expectedTotalWon()) {
            throw new ApiException(HttpStatus.CONFLICT, "PRICE_CHANGED",
                    "주문 금액이 바뀌었습니다. 새 금액을 확인해 주세요.");
        }
        return writer.save(memberId, cart.revision(), order);
    }

    private void requireItems(CartSnapshot cart) {
        if (cart.items().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_CART", "빈 장바구니는 주문할 수 없습니다.");
        }
    }

    private ShopOrder calculate(CartSnapshot cart, Map<String, ProductSnapshot> values) {
        ShopOrder order = ShopOrder.draft(cart.memberId());
        for (var item : cart.items()) order.addItem(values.get(item.code()), item.quantity());
        order.confirm(discountPolicy);
        return order;
    }

    @Transactional
    public OrderResponse cancel(long memberId, long id) {
        ShopOrder order = orders.findForUpdate(memberId, id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "주문을 찾을 수 없습니다."));
        order.cancel();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse list(long memberId, int page, int size) {
        if (page < 0 || size < 1 || size > 50
                || (long) page * size > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "page는 0 이상, size는 1~50이며 조회 시작 위치는 정수 범위여야 합니다.");
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<OrderSummaryResponse> result = orders.findSummaries(memberId, pageable);
        return OrderPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long memberId, long id) {
        ShopOrder order = orders.findDetailById(memberId, id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "주문을 찾을 수 없습니다."));
        return OrderResponse.from(order);
    }
}
