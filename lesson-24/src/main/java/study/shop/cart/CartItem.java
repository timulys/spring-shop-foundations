package study.shop.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints =
        @UniqueConstraint(columnNames = {"cart_id", "product_code"}))
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
    }

    CartItem(Cart cart, String productCode, int quantity) {
        this.cart = cart;
        this.productCode = productCode;
        changeQuantity(quantity);
    }

    void changeQuantity(int quantity) {
        Cart.checkQuantity(quantity);
        this.quantity = quantity;
    }

    void detach() {
        this.cart = null;
    }

    public Long getId() { return id; }
    public Cart getCart() { return cart; }
    public String getProductCode() { return productCode; }
    public int getQuantity() { return quantity; }
}
