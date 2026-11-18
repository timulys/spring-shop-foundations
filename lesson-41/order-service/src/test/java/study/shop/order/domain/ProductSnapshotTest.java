package study.shop.order.domain;

import study.shop.common.money.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProductSnapshotTest {
    @Test
    void valuesAreComparedWithoutEntityIdentity() {
        var first = new ProductSnapshot(" P001 ", "키보드", Money.ofWon(30_000));
        assertThat(first).isEqualTo(new ProductSnapshot("P001", "키보드", Money.ofWon(30_000)));
        assertThat(first.code()).isEqualTo("P001");
    }

    @Test
    void invalidNamesAndPriceAreRejectedAtTheBoundary() {
        assertThatThrownBy(() -> new ProductSnapshot(" ", "키보드", Money.ofWon(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductSnapshot("P001", " ", Money.ofWon(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductSnapshot("P001", "키보드", Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductSnapshot("P001", "키보드", Money.ofWon(1_000_001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProductSnapshot("P001", "키보드", null))
                .isInstanceOf(NullPointerException.class);
    }
}
