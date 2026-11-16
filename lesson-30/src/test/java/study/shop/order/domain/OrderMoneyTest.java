package study.shop.order.domain;

import study.shop.common.money.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrderMoneyTest {
    @Test
    void belowThresholdHasNoDiscountAndAddsShipping() {
        ShopOrder order = orderWithPrice(49_999);
        assertThat(order.getSubtotal()).isEqualTo(49_999);
        assertThat(order.getDiscount()).isZero();
        assertThat(order.getShippingFee()).isEqualTo(3_000);
        assertThat(order.getTotal()).isEqualTo(52_999);
    }

    @Test
    void exactThresholdGetsDiscountAndFreeShipping() {
        ShopOrder order = orderWithPrice(50_000);
        assertThat(order.getSubtotal()).isEqualTo(50_000);
        assertThat(order.getDiscount()).isEqualTo(5_000);
        assertThat(order.getShippingFee()).isZero();
        assertThat(order.getTotal()).isEqualTo(45_000);
    }

    @Test
    void exampleOrderKeepsSeventyThousandWonAfterCancellation() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new ProductSnapshot("P001", "키보드", Money.ofWon(30_000)), 2);
        order.addItem(new ProductSnapshot("P002", "마우스", Money.ofWon(15_000)), 1);
        order.confirm(new ThresholdFixedDiscountPolicy());
        assertThat(order.getItems().getFirst().lineAmount().won()).isEqualTo(60_000);
        assertThat(order.getSubtotal()).isEqualTo(75_000);
        assertThat(order.getDiscount()).isEqualTo(5_000);
        assertThat(order.getShippingFee()).isZero();
        assertThat(order.getTotal()).isEqualTo(70_000);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getTotal()).isEqualTo(70_000);
    }

    private ShopOrder orderWithPrice(long price) {
        ShopOrder order = ShopOrder.draft();
        order.addItem(new ProductSnapshot("P001", "경계값 상품", Money.ofWon(price)), 1);
        order.confirm(new ThresholdFixedDiscountPolicy());
        return order;
    }
}
