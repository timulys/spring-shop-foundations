package study.shop;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// 10강은 고정 학습용 사용자 한 명의 메모리 장바구니입니다.
// 앱을 재시작하면 비워집니다. 11강에서 DB 저장으로 전환합니다.
@Service
public class CartService {
    private final ProductService productService;
    private final Map<String, Integer> quantities = new TreeMap<>();

    public CartService(ProductService productService) {
        this.productService = productService;
    }

    public synchronized CartResponse get() {
        return snapshot(quantities);
    }

    public synchronized CartResponse add(String productCode, int quantity) {
        checkQuantity(quantity);
        String code = productCode.strip();
        requireProduct(code);
        int combined = quantities.getOrDefault(code, 0) + quantity;
        checkQuantity(combined);
        if (!quantities.containsKey(code) && quantities.size() >= 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_LIMIT",
                    "장바구니에는 서로 다른 상품을 100개까지 담을 수 있습니다.");
        }
        Map<String, Integer> next = new TreeMap<>(quantities);
        next.put(code, combined);
        return apply(next);
    }

    public synchronized CartResponse change(String code, int quantity) {
        checkQuantity(quantity);
        requireCartItem(code);
        Map<String, Integer> next = new TreeMap<>(quantities);
        next.put(code, quantity);
        return apply(next);
    }

    public synchronized CartResponse remove(String code) {
        requireCartItem(code);
        Map<String, Integer> next = new TreeMap<>(quantities);
        next.remove(code);
        return apply(next);
    }

    private CartResponse apply(Map<String, Integer> next) {
        CartResponse result = snapshot(next);
        quantities.clear();
        quantities.putAll(next);
        return result;
    }

    private CartResponse snapshot(Map<String, Integer> source) {
        var items = new ArrayList<CartResponse.Item>();
        int totalQuantity = 0;
        long subtotal = 0;
        for (var entry : source.entrySet()) {
            Product product = requireProduct(entry.getKey());
            long lineTotal = product.getUnitPrice() * entry.getValue();
            items.add(new CartResponse.Item(product.getCode(), product.getName(),
                    product.getUnitPrice(), entry.getValue(), lineTotal));
            totalQuantity += entry.getValue();
            subtotal += lineTotal;
        }
        return new CartResponse(items, totalQuantity, subtotal);
    }

    private Product requireProduct(String code) {
        return productService.findByCode(code).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "상품을 찾을 수 없습니다."));
    }

    private void requireCartItem(String code) {
        if (!quantities.containsKey(code)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                    "장바구니에 해당 상품이 없습니다.");
        }
    }

    private void checkQuantity(int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_LIMIT",
                    "상품별 합산 수량은 1개 이상 99개 이하여야 합니다.");
        }
    }
}
