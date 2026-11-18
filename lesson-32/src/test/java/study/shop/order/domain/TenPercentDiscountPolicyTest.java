package study.shop.order.domain;

import study.shop.common.money.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TenPercentDiscountPolicyTest {
    private final DiscountPolicy policy = new TenPercentDiscountPolicy();

    @Test
    void discountsTenPercentAndDropsFractionalWon() {
        assertThat(policy.calculateDiscount(Money.ofWon(75_000))).isEqualTo(Money.ofWon(7_500));
        assertThat(policy.calculateDiscount(Money.ofWon(49_999))).isEqualTo(Money.ofWon(4_999));
        assertThat(policy.calculateDiscount(Money.ofWon(9))).isEqualTo(Money.ZERO);
        assertThat(policy.calculateDiscount(Money.ZERO)).isEqualTo(Money.ZERO);
    }

    @Test
    void sameOrderCosts67500WithTheNewPolicy() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new ProductSnapshot("P001", "키보드", Money.ofWon(30_000)), 2);
        order.addItem(new ProductSnapshot("P002", "마우스", Money.ofWon(15_000)), 1);
        order.confirm(policy);
        assertThat(order.getSubtotal()).isEqualTo(75_000);
        assertThat(order.getDiscount()).isEqualTo(7_500);
        assertThat(order.getShippingFee()).isZero();
        assertThat(order.getTotal()).isEqualTo(67_500);
    }

    @Test
    void shippingStillUsesSubtotalBeforePercentageDiscount() {
        ShopOrder below = single(49_999);
        assertThat(below.getDiscount()).isEqualTo(4_999);
        assertThat(below.getShippingFee()).isEqualTo(3_000);
        assertThat(below.getTotal()).isEqualTo(48_000);
        ShopOrder threshold = single(50_000);
        assertThat(threshold.getShippingFee()).isZero();
        assertThat(threshold.getTotal()).isEqualTo(45_000);
    }

    @Test
    void divisionDoesNotOverflowAtLongBoundary() {
        assertThat(policy.calculateDiscount(Money.ofWon(Long.MAX_VALUE)).won())
                .isEqualTo(Long.MAX_VALUE / 10);
    }

    @Test
    void missingSubtotalIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> policy.calculateDiscount(null));
    }

    private ShopOrder single(long price) {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new ProductSnapshot("TEST", "정책 검사 상품", Money.ofWon(price)), 1);
        order.confirm(policy);
        return order;
    }
}
