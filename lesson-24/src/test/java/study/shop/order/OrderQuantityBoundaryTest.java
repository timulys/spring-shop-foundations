package study.shop.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import study.shop.catalog.Product;
import study.shop.common.ApiException;
import static org.junit.jupiter.api.Assertions.*;

class OrderQuantityBoundaryTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void rejectsQuantityOutsideOrderRange(int quantity) {
        ShopOrder order = new ShopOrder();
        ApiException error = assertThrows(ApiException.class, () ->
                order.addItem(new Product("Q", "검증 상품", 30000L, "검증"), quantity));
        assertEquals("QUANTITY_LIMIT", error.code());
        assertTrue(order.getItems().isEmpty());
    }
}
