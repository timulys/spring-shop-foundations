package study.shop.order;

import study.shop.catalog.Product;
import study.shop.common.ApiException;
import study.shop.common.money.Money;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "orders")
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.DRAFT;
    @Column(name = "payment_mode", nullable = false, length = 20)
    private String paymentMode = "SIMULATED";

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @OrderBy("productCode ASC")
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "subtotal_won", nullable = false)
    private long subtotal;
    @Column(name = "discount_won", nullable = false)
    private long discount;
    @Column(name = "shipping_fee_won", nullable = false)
    private long shippingFee;
    @Column(name = "total_won", nullable = false)
    private long total;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

    protected ShopOrder() {
    }

    void addItem(Product product, int quantity) {
        requireDraft();
        if (items.size() >= 100 || items.stream().anyMatch(
                item -> item.getProductCode().equals(product.getCode()))) {
            throw new IllegalArgumentException("주문 항목의 개수나 상품 중복을 확인해 주세요.");
        }
        items.add(new OrderItem(this, product, quantity));
    }

    void confirm() {
        requireDraft();
        if (items.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_CART",
                    "빈 장바구니는 주문할 수 없습니다.");
        }
        Money subtotalAmount = Money.ZERO;
        for (OrderItem item : items) {
            subtotalAmount = subtotalAmount.plus(item.lineAmount());
        }
        boolean eligible = subtotalAmount.isAtLeast(Money.ofWon(50_000));
        Money discountAmount = eligible ? Money.ofWon(5_000) : Money.ZERO;
        Money shippingAmount = eligible ? Money.ZERO : Money.ofWon(3_000);
        Money totalAmount = subtotalAmount.minus(discountAmount).plus(shippingAmount);

        subtotal = subtotalAmount.won();
        discount = discountAmount.won();
        shippingFee = shippingAmount.won();
        total = totalAmount.won();
        status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != OrderStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_STATE_CONFLICT",
                    "확정 상태의 주문만 취소할 수 있습니다.");
        }
        status = OrderStatus.CANCELLED;
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_STATE_CONFLICT",
                    "이미 확정한 주문의 항목이나 금액은 바꿀 수 없습니다.");
        }
    }

    public Long getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentMode() { return paymentMode; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public long getSubtotal() { return subtotal; }
    public long getDiscount() { return discount; }
    public long getShippingFee() { return shippingFee; }
    public long getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
}
