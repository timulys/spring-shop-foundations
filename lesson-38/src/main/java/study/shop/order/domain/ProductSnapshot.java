package study.shop.order.domain;

import study.shop.common.money.Money;
import java.util.Objects;

/** 주문이 필요한 상품 값만 전달합니다. JPA 엔티티가 아닙니다. */
public record ProductSnapshot(String code, String name, Money unitPrice) {
    public ProductSnapshot {
        if (code == null || code.isBlank() || code.strip().length() > 20) {
            throw new IllegalArgumentException("상품 코드는 1~20자여야 합니다.");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("상품명은 1~100자여야 합니다.");
        }
        Objects.requireNonNull(unitPrice, "상품 단가가 필요합니다.");
        if (unitPrice.won() < 1 || unitPrice.won() > 1_000_000) {
            throw new IllegalArgumentException("단가는 1~1000000원이어야 합니다.");
        }
        code = code.strip();
    }
}
