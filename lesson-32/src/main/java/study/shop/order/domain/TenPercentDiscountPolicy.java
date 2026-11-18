package study.shop.order.domain;

import study.shop.common.money.Money;
import java.util.Objects;

/** 원 미만은 버립니다. 기본 앱 설정은 기존 정액 할인 정책을 유지합니다. */
public final class TenPercentDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculateDiscount(Money subtotal) {
        Objects.requireNonNull(subtotal, "상품 합계가 필요합니다.");
        return Money.ofWon(subtotal.won() / 10);
    }
}
