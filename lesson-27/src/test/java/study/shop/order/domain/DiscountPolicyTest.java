package study.shop.order.domain;

import study.shop.catalog.domain.Product;
import study.shop.common.money.Money;
import study.shop.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DiscountPolicyTest {
    private final DiscountPolicy standard = new ThresholdFixedDiscountPolicy();
    private final DiscountPolicy none = new NoDiscountPolicy();

    @Test
    void fixedDiscountIncludesTheThreshold() {
        assertThat(standard.calculateDiscount(Money.ofWon(49_999))).isEqualTo(Money.ZERO);
        assertThat(standard.calculateDiscount(Money.ofWon(50_000))).isEqualTo(Money.ofWon(5_000));
        assertThat(standard.calculateDiscount(Money.ofWon(50_001))).isEqualTo(Money.ofWon(5_000));
    }

    @Test
    void noDiscountReturnsZeroForEverySubtotal() {
        for (long subtotal : new long[] {0, 49_999, 50_000, 75_000}) {
            assertThat(none.calculateDiscount(Money.ofWon(subtotal))).isEqualTo(Money.ZERO);
        }
    }

    @Test
    void sameCartCosts70000Or75000DependingOnPolicy() {
        ShopOrder discounted = sample(standard);
        ShopOrder fullPrice = sample(none);
        assertThat(discounted.getSubtotal()).isEqualTo(75_000);
        assertThat(fullPrice.getSubtotal()).isEqualTo(75_000);
        assertThat(discounted.getTotal()).isEqualTo(70_000);
        assertThat(fullPrice.getTotal()).isEqualTo(75_000);
        assertThat(discounted.getShippingFee()).isZero();
        assertThat(fullPrice.getShippingFee()).isZero();
    }

    @Test
    void keyboardTwiceCosts55000Or60000() {
        ShopOrder discounted = single(30_000, 2, standard);
        ShopOrder fullPrice = single(30_000, 2, none);
        assertThat(discounted.getTotal()).isEqualTo(55_000);
        assertThat(fullPrice.getTotal()).isEqualTo(60_000);
        assertThat(discounted.getShippingFee()).isZero();
        assertThat(fullPrice.getShippingFee()).isZero();
    }

    @Test
    void shippingStillUsesSubtotalBeforeDiscount() {
        for (DiscountPolicy policy : new DiscountPolicy[] {standard, none}) {
            ShopOrder order = single(49_999, 1, policy);
            assertThat(order.getDiscount()).isZero();
            assertThat(order.getShippingFee()).isEqualTo(3_000);
            assertThat(order.getTotal()).isEqualTo(52_999);
        }
        assertThat(single(50_000, 1, standard).getTotal()).isEqualTo(45_000);
        assertThat(single(50_000, 1, none).getTotal()).isEqualTo(50_000);
    }

    @Test
    void excessiveDiscountCannotPartlyConfirmAnOrder() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new Product("P001", "키보드", 30_000, "주변기기"), 1);
        assertThatThrownBy(() -> order.confirm(subtotal -> Money.ofWon(30_001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getSubtotal()).isZero();
        assertThat(order.getTotal()).isZero();
    }

    @Test
    void nullPolicyResultCannotConfirmAnOrder() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new Product("P001", "키보드", 30_000, "주변기기"), 1);
        assertThatThrownBy(() -> order.confirm(subtotal -> null))
                .isInstanceOf(NullPointerException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getTotal()).isZero();
    }

    @Test
    void savedDiscountAndTotalRemainAfterCancellation() {
        ShopOrder order = sample(standard);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getDiscount()).isEqualTo(5_000);
        assertThat(order.getTotal()).isEqualTo(70_000);
        assertThatThrownBy(() -> order.confirm(none)).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("ORDER_STATE_CONFLICT");
        assertThat(order.getTotal()).isEqualTo(70_000);
    }

    private ShopOrder sample(DiscountPolicy policy) {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new Product("P001", "키보드", 30_000, "주변기기"), 2);
        order.addItem(new Product("P002", "마우스", 15_000, "주변기기"), 1);
        order.confirm(policy);
        return order;
    }

    private ShopOrder single(long price, int quantity, DiscountPolicy policy) {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new Product("TEST", "정책 검사 상품", price, "검사"), quantity);
        order.confirm(policy);
        return order;
    }
}
