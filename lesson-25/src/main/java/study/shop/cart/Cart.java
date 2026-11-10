package study.shop.cart;

import study.shop.common.ApiException;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    private Long id;

    @Column(nullable = false)
    private long revision;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.PERSIST,
            orphanRemoval = true)
    @OrderBy("productCode ASC")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    Cart(long id) {
        this.id = id;
    }

    public void add(String productCode, int quantity) {
        checkQuantity(quantity);
        CartItem existing = items.stream()
                .filter(item -> item.getProductCode().equals(productCode))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.changeQuantity(existing.getQuantity() + quantity);
            revision++;
            return;
        }
        if (items.size() >= 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_LIMIT",
                    "장바구니에는 서로 다른 상품을 100개까지 담을 수 있습니다.");
        }
        CartItem item = new CartItem(this, productCode, quantity);
        items.add(item);
        revision++;
    }

    public void change(String code, int quantity) {
        checkQuantity(quantity);
        requireItem(code).changeQuantity(quantity);
        revision++;
    }

    public void remove(String code) {
        CartItem item = requireItem(code);
        items.remove(item);
        item.detach();
        revision++;
    }

    public void clear() {
        for (CartItem item : items) item.detach();
        items.clear();
        revision++;
    }

    private CartItem requireItem(String code) {
        return items.stream().filter(item -> item.getProductCode().equals(code))
                .findFirst().orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                        "장바구니에 해당 상품이 없습니다."));
    }

    static void checkQuantity(int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_LIMIT",
                    "상품별 합산 수량은 1개 이상 99개 이하여야 합니다.");
        }
    }

    public long getRevision() { return revision; }
    public Long getId() { return id; }
    public List<CartItem> getItems() { return List.copyOf(items); }
}
