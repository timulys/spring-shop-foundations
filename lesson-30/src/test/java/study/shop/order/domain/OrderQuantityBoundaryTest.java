package study.shop.order.domain;

import study.shop.common.money.Money;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import study.shop.common.ApiException;
import static org.junit.jupiter.api.Assertions.*;

class OrderQuantityBoundaryTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void rejectsQuantityOutsideOrderRange(int quantity) {
        ShopOrder order = ShopOrder.draft();
        ApiException error = assertThrows(ApiException.class, () ->
                order.addItem(new ProductSnapshot("Q", "검증 상품", Money.ofWon(30000L)), quantity));
        assertEquals("QUANTITY_LIMIT", error.code());
        assertTrue(order.getItems().isEmpty());
    }
}
