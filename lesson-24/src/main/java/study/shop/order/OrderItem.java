package study.shop.order;

import study.shop.catalog.Product;
import study.shop.common.ApiException;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items", uniqueConstraints =
        @UniqueConstraint(columnNames = {"order_id", "product_code"}))
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    @Column(name = "unit_price_won", nullable = false)
    private long unitPrice;
    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    OrderItem(ShopOrder order, Product product, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "QUANTITY_LIMIT", "상품별 합산 수량은 1개 이상 99개 이하여야 합니다.");
        }
        this.order = order;
        this.productCode = product.getCode();
        this.productName = product.getName();
        this.unitPrice = product.getUnitPrice();
        this.quantity = quantity;
    }

    public long lineTotal() { return unitPrice * quantity; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public long getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
