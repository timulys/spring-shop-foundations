package study.shop.catalog;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code;
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    @Column(name = "unit_price_won", nullable = false)
    private long unitPrice;
    @Column(name = "category", length = 40, nullable = false)
    private String category;

    protected Product() {
    }

    public Product(
            String code, String name,
            long unitPrice, String category) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("상품 코드는 비어 있을 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 비어 있을 수 없습니다.");
        }
        if (unitPrice < 1L || unitPrice > 1_000_000L) {
            throw new IllegalArgumentException("단가는 1~1000000원이어야 합니다.");
        }
        this.code = code.strip();
        this.name = name;
        this.unitPrice = unitPrice;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public String getCategory() {
        return category;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Product other = (Product) obj;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
