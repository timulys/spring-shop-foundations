package study.shop;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final CartRepository carts;
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(CartRepository carts, ProductRepository products,
                        OrderRepository orders) {
        this.carts = carts;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public OrderPreviewResponse preview() {
        Cart cart = carts.findForUpdate(1L).orElseThrow();
        return OrderPreviewResponse.from(cart, calculate(cart));
    }

    @Transactional
    public OrderResponse place(PlaceOrderRequest request) {
        Cart cart = carts.findForUpdate(1L).orElseThrow();
        ShopOrder order = calculate(cart);
        if (cart.getRevision() != request.cartRevision()) {
            throw new ApiException(HttpStatus.CONFLICT, "CART_CHANGED",
                    "장바구니가 바뀌었습니다. 주문 내용을 다시 확인해 주세요.");
        }
        if (order.getTotal() != request.expectedTotalWon()) {
            throw new ApiException(HttpStatus.CONFLICT, "PRICE_CHANGED",
                    "주문 금액이 바뀌었습니다. 새 금액을 확인해 주세요.");
        }
        // 학습용 모의 승인입니다. 실제 결제 서버를 호출하지 않습니다.
        orders.save(order);
        cart.clear();
        return OrderResponse.from(order);
    }

    private ShopOrder calculate(Cart cart) {
        ShopOrder order = new ShopOrder();
        for (CartItem item : cart.getItems()) {
            Product product = products.findById(item.getProductCode())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
            order.addItem(product, item.getQuantity());
        }
        order.confirm();
        return order;
    }

    @Transactional
    public OrderResponse cancel(long id) {
        ShopOrder order = orders.findForUpdate(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "주문을 찾을 수 없습니다."));
        order.cancel();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > 50
                || (long) page * size > Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "page는 0 이상, size는 1~50이며 조회 시작 위치는 정수 범위여야 합니다.");
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<OrderSummaryResponse> result = orders.findSummaries(pageable);
        return OrderPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long id) {
        ShopOrder order = orders.findDetailById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "주문을 찾을 수 없습니다."));
        return OrderResponse.from(order);
    }
}
