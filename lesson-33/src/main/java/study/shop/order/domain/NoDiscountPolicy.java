package study.shop.order.domain;

import study.shop.common.money.Money;
import java.util.Objects;

public final class NoDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculateDiscount(Money subtotal) {
        Objects.requireNonNull(subtotal, "상품 합계가 필요합니다.");
        return Money.ZERO;
    }
}
