package study.shop.order.service;

import study.shop.cart.domain.Cart;
import study.shop.common.CatalogServerTestBase;
import study.shop.order.domain.DiscountPolicy;
import study.shop.order.domain.ShopOrder;

/** Prepared values for testing only the local write transaction. No remote call occurs here. */
public final class OrderWriteFixtures {
    private OrderWriteFixtures() {}
    public static ShopOrder prepared(Cart cart, DiscountPolicy policy) {
        ShopOrder order = ShopOrder.draft(cart.getMemberId());
        for (var item : cart.getItems()) {
            order.addItem(CatalogServerTestBase.catalogSnapshot(item.getProductCode()), item.getQuantity());
        }
        order.confirm(policy);
        return order;
    }
}
