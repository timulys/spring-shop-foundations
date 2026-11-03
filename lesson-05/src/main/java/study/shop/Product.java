package study.shop;

import java.util.Objects;

public class Product {
    private final String code;
    private final String name;
    private final long unitPrice;
    private final String category;

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
