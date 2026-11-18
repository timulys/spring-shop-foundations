package study.shop.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import study.shop.cart.service.CartService;
import study.shop.common.ApiException;
import study.shop.order.dto.OrderResponse;
import study.shop.order.dto.PlaceOrderRequest;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.jdbc.Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderSnapshotTest extends study.shop.common.CatalogServerTestBase {
    @Autowired study.shop.cart.service.CartStore testCartStore;
    @Autowired CartService carts;
    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;

    private OrderResponse placeSample() {
        carts.add(100001L, "P001", 2, testCartStore.read(100001L).revision());
        carts.add(100001L, "P002", 1, testCartStore.read(100001L).revision());
        return service.place(100001L, OrderTestRequests.current(service), java.util.UUID.randomUUID().toString());
    }

    @Test
    void preservesPurchasedNamePriceAndQuantityAfterCatalogChange() {
        OrderResponse original = placeSample();
        assertEquals(75000, original.subtotal());
        assertEquals(5000, original.discount());
        assertEquals(0, original.shippingFee());
        assertEquals(70000, original.total());
        CATALOG.set("P001", "새 키보드", 40000, "주변기기");
        var reloaded = service.get(100001L, original.id());
        assertEquals(original, reloaded);
        assertEquals("키보드", reloaded.items().getFirst().productName());
        assertEquals(30000, reloaded.items().getFirst().unitPrice());
        assertEquals(2, reloaded.items().getFirst().quantity());
    }

    @Test
    void storesStateAsNameAndClearsTheCart() {
        var order = placeSample();
        assertEquals("CONFIRMED", jdbc.queryForObject("select status from orders where id=?", String.class, order.id()));
        assertEquals("SIMULATED", service.get(100001L, order.id()).paymentMode());
        assertEquals(0, jdbc.queryForObject("select count(*) from cart_items", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from order_items where order_id=?", Integer.class, order.id()));
    }

    @Test
    void readsAnOldOrderWhenCatalogIsUnavailableAndNoLocalProductsTableExists() {
        var order = placeSample();
        int before = CATALOG.calls.size();
        CATALOG.failAll = true;
        assertEquals(order, service.get(100001L, order.id()));
        assertEquals(before, CATALOG.calls.size());
        assertEquals(0, jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name='products'", Integer.class));
    }

    @Test
    void rejectsAnEmptyCartBeforeSavingAnOrder() {
        var error = assertThrows(ApiException.class, () -> service.place(100001L, new PlaceOrderRequest(jdbc.queryForObject("select revision from carts where member_id=100001", Long.class), 0L), java.util.UUID.randomUUID().toString()));
        assertEquals("EMPTY_CART", error.code());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(0, CATALOG.batchCalls.get());
    }
}
