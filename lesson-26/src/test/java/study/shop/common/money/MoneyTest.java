package study.shop.common.money;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    @Test
    void sameWonHasValueEquality() {
        Money first = Money.ofWon(30_000);
        Money second = Money.ofWon(30_000);
        assertThat(first).isNotSameAs(second).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first).isNotEqualTo(Money.ofWon(30_001));
    }

    @Test
    void calculationLeavesOriginalValuesUnchanged() {
        Money unitPrice = Money.ofWon(30_000);
        Money shipping = Money.ofWon(3_000);
        Money result = unitPrice.times(2).plus(shipping).minus(Money.ofWon(5_000));
        assertThat(result.won()).isEqualTo(58_000);
        assertThat(unitPrice.won()).isEqualTo(30_000);
        assertThat(shipping.won()).isEqualTo(3_000);
    }

    @Test
    void zeroIsValidForDiscountAndShipping() {
        assertThat(Money.ofWon(0)).isEqualTo(Money.ZERO);
        assertThat(Money.ofWon(3_000).minus(Money.ofWon(3_000))).isEqualTo(Money.ZERO);
        assertThat(Money.ofWon(3_000).times(0)).isEqualTo(Money.ZERO);
    }

    @Test
    void negativeMoneyIsRejectedThroughEveryConstructorPath() {
        assertThatThrownBy(() -> Money.ofWon(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subtractingMoreThanAmountIsRejected() {
        assertThatThrownBy(() -> Money.ofWon(3_000).minus(Money.ofWon(5_000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeMultiplierIsRejectedEvenForZeroMoney() {
        assertThatThrownBy(() -> Money.ZERO.times(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void additionOverflowIsRejected() {
        assertThatThrownBy(() -> Money.ofWon(Long.MAX_VALUE).plus(Money.ofWon(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void multiplicationOverflowIsRejected() {
        assertThatThrownBy(() -> Money.ofWon(Long.MAX_VALUE).times(2))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void largestRepresentableAmountRemainsExact() {
        assertThat(Money.ofWon(Long.MAX_VALUE).plus(Money.ZERO).won())
                .isEqualTo(Long.MAX_VALUE);
        assertThat(Money.ofWon(Long.MAX_VALUE).times(1).won()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void thresholdIncludesExactBoundary() {
        Money threshold = Money.ofWon(50_000);
        assertThat(Money.ofWon(49_999).isAtLeast(threshold)).isFalse();
        assertThat(Money.ofWon(50_000).isAtLeast(threshold)).isTrue();
        assertThat(Money.ofWon(50_001).isAtLeast(threshold)).isTrue();
    }
}
