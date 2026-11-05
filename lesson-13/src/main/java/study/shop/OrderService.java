package study.shop;

import org.springframework.http.HttpStatus;
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
    public OrderResponse place() {
        Cart cart = carts.findForUpdate(1L).orElseThrow();
        ShopOrder order = new ShopOrder();
        for (CartItem item : cart.getItems()) {
            Product product = products.findById(item.getProductCode())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."));
            order.addItem(product, item.getQuantity());
        }
        // 학습용 모의 승인입니다. 실제 결제 서버를 호출하지 않습니다.
        order.confirm();
        orders.save(order);
        cart.clear();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(long id) {
        ShopOrder order = orders.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "주문을 찾을 수 없습니다."));
        return OrderResponse.from(order);
    }
}
