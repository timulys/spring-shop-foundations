package study.shop.cart.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import study.shop.cart.dto.CartResponse;
import study.shop.common.ApiException;
import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;
import study.shop.order.service.port.ProductLookup;

@Service
@Transactional(propagation = Propagation.NEVER)
public class CartService {
    private final CartStore carts;
    private final ProductLookup products;
    public CartService(CartStore carts, ProductLookup products) {
        this.carts = carts;
        this.products = products;
    }

    public CartResponse get(long memberId) {
        CartSnapshot cart = carts.read(memberId);
        return response(cart, products.requireAll(cart.codes()));
    }

    public CartResponse add(long memberId, String productCode, int quantity) {
        String code = productCode.strip();
        CartSnapshot before = carts.read(memberId);
        List<String> codes = new ArrayList<>(before.codes());
        if (!codes.contains(code)) codes.add(code);
        if (codes.size() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_LIMIT",
                    "장바구니에는 서로 다른 상품을 100개까지 담을 수 있습니다.");
        }
        var values = products.requireAll(codes);
        CartSnapshot after = carts.change(memberId, before.revision(), cart -> cart.add(code, quantity));
        return response(after, values);
    }

    public CartResponse change(long memberId, String code, int quantity) {
        CartSnapshot before = carts.read(memberId);
        requireItem(before, code);
        var values = products.requireAll(before.codes());
        CartSnapshot after = carts.change(memberId, before.revision(), cart -> cart.change(code, quantity));
        return response(after, values);
    }

    public CartResponse remove(long memberId, String code) {
        CartSnapshot before = carts.read(memberId);
        requireItem(before, code);
        var remaining = before.codes().stream().filter(value -> !value.equals(code)).toList();
        var values = products.requireAll(remaining);
        CartSnapshot after = carts.change(memberId, before.revision(), cart -> cart.remove(code));
        return response(after, values);
    }

    private void requireItem(CartSnapshot cart, String code) {
        if (!cart.codes().contains(code)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                    "장바구니에 해당 상품이 없습니다.");
        }
    }

    private CartResponse response(CartSnapshot cart, Map<String, ProductSnapshot> values) {
        var items = new ArrayList<CartResponse.Item>();
        int totalQuantity = 0;
        Money subtotal = Money.ZERO;
        for (var item : cart.items().stream().sorted(Comparator.comparing(CartSnapshot.Item::code)).toList()) {
            ProductSnapshot product = values.get(item.code());
            Money lineTotal = product.unitPrice().times(item.quantity());
            items.add(new CartResponse.Item(product.code(), product.name(), product.unitPrice().won(),
                    item.quantity(), lineTotal.won()));
            totalQuantity += item.quantity();
            subtotal = subtotal.plus(lineTotal);
        }
        return new CartResponse(items, totalQuantity, subtotal.won());
    }
}
