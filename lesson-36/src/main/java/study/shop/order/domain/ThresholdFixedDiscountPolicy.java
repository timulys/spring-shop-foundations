package study.shop.order.domain;

import study.shop.common.money.Money;
import java.util.Objects;

public final class ThresholdFixedDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculateDiscount(Money subtotal) {
        Objects.requireNonNull(subtotal, "상품 합계가 필요합니다.");
        return subtotal.isAtLeast(Money.ofWon(50_000))
                ? Money.ofWon(5_000) : Money.ZERO;
    }
}
