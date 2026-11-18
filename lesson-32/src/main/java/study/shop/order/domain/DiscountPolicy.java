package study.shop.order.domain;

import study.shop.common.money.Money;

@FunctionalInterface
public interface DiscountPolicy {
    Money calculateDiscount(Money subtotal);
}
