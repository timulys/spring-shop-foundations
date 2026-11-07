package study.shop;

import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private static final long LEARNING_CART_ID = 1L;
    private final CartRepository cartRepository;
    private final ProductService productService;

    public CartService(CartRepository cartRepository, ProductService productService) {
        this.cartRepository = cartRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public CartResponse get() {
        Cart cart = cartRepository.findById(LEARNING_CART_ID).orElseThrow();
        return snapshot(cart);
    }

    @Transactional
    public CartResponse add(String productCode, int quantity) {
        String code = productCode.strip();
        Cart cart = loadForChange();
        requireProduct(code);
        cart.add(code, quantity);
        return snapshot(cart);
    }

    @Transactional
    public CartResponse change(String code, int quantity) {
        Cart cart = loadForChange();
        cart.change(code, quantity);
        return snapshot(cart);
    }

    @Transactional
    public CartResponse remove(String code) {
        Cart cart = loadForChange();
        cart.remove(code);
        return snapshot(cart);
    }

    private Cart loadForChange() {
        return cartRepository.findForUpdate(LEARNING_CART_ID).orElseThrow();
    }

    private CartResponse snapshot(Cart cart) {
        var items = new ArrayList<CartResponse.Item>();
        int totalQuantity = 0;
        long subtotal = 0;
        for (CartItem item : cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getProductCode)).toList()) {
            Product product = requireProduct(item.getProductCode());
            long lineTotal = product.getUnitPrice() * item.getQuantity();
            items.add(new CartResponse.Item(product.getCode(), product.getName(),
                    product.getUnitPrice(), item.getQuantity(), lineTotal));
            totalQuantity += item.getQuantity();
            subtotal += lineTotal;
        }
        return new CartResponse(items, totalQuantity, subtotal);
    }

    private Product requireProduct(String code) {
        return productService.findByCode(code).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "상품을 찾을 수 없습니다."));
    }
}
