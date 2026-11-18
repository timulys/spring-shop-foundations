package study.shop.order.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import study.shop.cart.service.CartService;
import study.shop.cart.service.CartStore;
import study.shop.common.ApiException;
import study.shop.order.dto.PlaceOrderRequest;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.jdbc.Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RemoteOrderBoundaryTest extends study.shop.common.CatalogServerTestBase {
    @Autowired CartService carts;
    @Autowired CartStore store;
    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;

    private PlaceOrderRequest prepare() {
        carts.add(100001L, "P001", 2);
        carts.add(100001L, "P002", 1);
        return OrderTestRequests.current(orders);
    }

    @Test
    void checkoutCannotJoinACallersTransactionThatMightAlreadyHoldLocks() {
        var request = prepare();
        int calls = CATALOG.calls.size();
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> new TransactionTemplate(manager).execute(status -> orders.place(100001L, request)));
        assertEquals(calls, CATALOG.calls.size());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
    }

    @Test
    void onePreviewUsesOneBatchForBothProducts() {
        prepare();
        CATALOG.batchCalls.set(0);
        assertEquals(70000, orders.preview(100001L).total());
        assertEquals(1, CATALOG.batchCalls.get());
    }

    @Test
    void missingOneProductRejectsTheWholeOrderAndKeepsCart() {
        var request = prepare();
        CATALOG.products.remove("P002");
        var error = assertThrows(ApiException.class, () -> orders.place(100001L, request));
        assertEquals("PRODUCT_NOT_FOUND", error.code());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(2, store.read(100001L).items().size());
    }

    @Test
    void remoteFailurePreventsCartAndOrderWrites() {
        var request = prepare();
        var before = store.read(100001L);
        CATALOG.failAll = true;
        assertThrows(ApiException.class, () -> carts.add(100001L, "P003", 1));
        assertThrows(ApiException.class, () -> orders.place(100001L, request));
        assertEquals(before, store.read(100001L));
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
    }

    @Test
    void remoteWaitDoesNotHoldCartWriteLockAndLaterRevisionCheckRejectsStaleOrder() throws Exception {
        var request = prepare();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        CATALOG.beforeBatch = () -> {
            entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release not signalled"); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
        };
        var pending = CompletableFuture.supplyAsync(() -> {
            try { orders.place(100001L, request); return "SAVED"; }
            catch (ApiException ex) { return ex.code(); }
        });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                jdbc.execute("set local lock_timeout = '500ms'");
                store.change(100001L, request.cartRevision(), cart -> cart.change("P001", 3));
            });
        } finally {
            release.countDown();
            CATALOG.beforeBatch = null;
        }
        assertEquals("CART_CHANGED", pending.get(5, TimeUnit.SECONDS));
        assertEquals(0, jdbc.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(3, jdbc.queryForObject("select quantity from cart_items where product_code='P001'", Integer.class));
    }

    @Test
    void historicalReadAndCancellationDoNotCallTheUnavailableCatalog() {
        var request = prepare();
        var order = orders.place(100001L, request);
        int calls = CATALOG.calls.size();
        CATALOG.failAll = true;
        assertEquals(order, orders.get(100001L, order.id()));
        assertEquals("CANCELLED", orders.cancel(100001L, order.id()).status().name());
        assertEquals(calls, CATALOG.calls.size());
    }
}
