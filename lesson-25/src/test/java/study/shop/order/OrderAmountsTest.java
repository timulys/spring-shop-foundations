package study.shop.order;

import study.shop.catalog.Product;
import study.shop.common.ApiException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderAmountsTest {
    private ShopOrder one(long price) {
        ShopOrder order = new ShopOrder();
        order.addItem(new Product("TEST", "검증 상품", price, "검증"), 1);
        order.confirm();
        return order;
    }

    @Test
    void appliesShippingBelowTheDiscountThreshold() {
        ShopOrder order = one(49_999L);
        assertEquals(0L, order.getDiscount());
        assertEquals(3_000L, order.getShippingFee());
        assertEquals(52_999L, order.getTotal());
    }

    @Test
    void appliesDiscountAtTheExactThreshold() {
        ShopOrder order = one(50_000L);
        assertEquals(5_000L, order.getDiscount());
        assertEquals(0L, order.getShippingFee());
        assertEquals(45_000L, order.getTotal());
    }

    @Test
    void keepsLargeAmountsInLongFields() {
        ShopOrder order = new ShopOrder();
        for (int i = 0; i < 100; i++) {
            order.addItem(new Product("Q" + i, "검증 상품", 1_000_000L, "검증"), 99);
        }
        order.confirm();
        assertEquals(9_900_000_000L, order.getSubtotal());
        assertEquals(9_899_995_000L, order.getTotal());
    }

    @Test
    void protectsConfirmedAmountsAndTheItemList() {
        ShopOrder order = one(30_000L);
        assertThrows(ApiException.class, order::confirm);
        assertThrows(ApiException.class, () ->
                order.addItem(new Product("NEW", "새 상품", 10_000L, "검증"), 1));
        assertThrows(UnsupportedOperationException.class, () -> order.getItems().clear());
        assertEquals(33_000L, order.getTotal());
    }
}
