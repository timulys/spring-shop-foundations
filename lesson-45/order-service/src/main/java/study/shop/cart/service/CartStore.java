package study.shop.cart.service;

import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.shop.cart.domain.Cart;
import study.shop.cart.repository.CartRepository;
import study.shop.common.ApiException;

@Service
public class CartStore {
    private final CartRepository carts;
    public CartStore(CartRepository carts) { this.carts = carts; }

    @Transactional(readOnly = true)
    public CartSnapshot read(long memberId) {
        return CartSnapshot.from(carts.findWithItems(memberId).orElseThrow());
    }

    @Transactional
    public CartSnapshot change(long memberId, long expectedRevision, Consumer<Cart> operation) {
        Cart cart = carts.findWithItems(memberId).orElseThrow();
        requireRevision(cart.getRevision(), expectedRevision);
        operation.accept(cart);
        return CartSnapshot.from(cart);
    }

    public static void requireExpectedRevision(Long expected) {
        if (expected == null || expected < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CART_REVISION",
                    "장바구니 버전을 함께 보내 주세요.");
        }
    }

    public static ApiException changed() {
        return new ApiException(HttpStatus.CONFLICT, "CART_CHANGED",
                "장바구니가 바뀌었습니다. 새로 조회한 뒤 다시 진행해 주세요.");
    }

    public static void requireRevision(long current, long expected) {
        if (current != expected) {
            throw changed();
        }
    }
}
