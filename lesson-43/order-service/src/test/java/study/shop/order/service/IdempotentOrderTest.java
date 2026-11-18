package study.shop.order.service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import study.shop.cart.service.*;
import study.shop.cart.repository.CartRepository;
import study.shop.common.ApiException;
import study.shop.order.dto.*;
import study.shop.order.domain.DiscountPolicy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class IdempotentOrderTest extends study.shop.common.CatalogServerTestBase {
    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired CartStore store;
    @Autowired CartRepository cartRows;
    @Autowired OrderWriter writer;
    @Autowired DiscountPolicy discount;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoSpyBean OrderRequestReader requests;
    private String key() { return UUID.randomUUID().toString(); }
    private PlaceOrderRequest prepare(long member) {
        carts.add(member, "P001", 2, store.read(member).revision());
        carts.add(member, "P002", 1, store.read(member).revision());
        var preview = orders.preview(member);
        return new PlaceOrderRequest(preview.cartRevision(), preview.total());
    }
    private int count(String table) { return jdbc.queryForObject("select count(*) from " + table, Integer.class); }
    private void assertOneOrder() {
        assertEquals(1, count("orders")); assertEquals(1, count("order_requests"));
        assertEquals(2, count("order_items")); assertEquals(0, count("cart_items"));
    }

    @Test
    void replayWorksAfterCartClearAndWithCatalogUnavailable() {
        var request = prepare(100001L); String key = key();
        var first = orders.place(100001L, request, key);
        int calls = CATALOG.batchCalls.get(); CATALOG.failAll = true;
        assertEquals(first, orders.place(100001L, request, key));
        assertEquals(calls, CATALOG.batchCalls.get()); assertOneOrder();
        orders.cancel(100001L, first.id());
        assertEquals("CANCELLED", orders.place(100001L, request, key).status().name());
    }

    @Test
    void changedContentWithAnExistingKeyIs409WithoutRemoteLookup() {
        var request = prepare(100001L); String key = key(); orders.place(100001L, request, key);
        int calls = CATALOG.batchCalls.get(); CATALOG.failAll = true;
        for (var other : List.of(new PlaceOrderRequest(request.cartRevision()+1, request.expectedTotalWon()),
                new PlaceOrderRequest(request.cartRevision(), request.expectedTotalWon()+1))) {
            var error = assertThrows(ApiException.class, () -> orders.place(100001L, other, key));
            assertEquals(409, error.status().value()); assertEquals("IDEMPOTENCY_KEY_REUSED", error.code());
        }
        assertEquals(calls, CATALOG.batchCalls.get()); assertOneOrder();
    }

    @Test
    void twoSimultaneousRequestsWithTheSameKeyReturnTheSameOrder() throws Exception {
        var request = prepare(100001L); String key = key();
        var results = race(() -> orders.place(100001L, request, key), () -> orders.place(100001L, request, key));
        assertEquals(results.get(0).id(), results.get(1).id()); assertOneOrder();
        System.out.println("같은 키 동시 요청: 같은 주문 번호 " + results.get(0).id() + ", orders=1, requestRecords=1");
    }

    @Test
    void differentKeysCannotOrderTheSameCartTwice() throws Exception {
        var request = prepare(100001L);
        var results = race(() -> placeResult(request, key()), () -> placeResult(request, key()));
        assertEquals(1, results.stream().filter(value -> value.equals("SAVED")).count());
        assertEquals(1, results.stream().filter(value -> value.equals("CART_CHANGED")).count());
        assertOneOrder();
    }

    private String placeResult(PlaceOrderRequest request, String key) {
        try { orders.place(100001L, request, key); return "SAVED"; }
        catch (ApiException error) { assertEquals(409, error.status().value()); return error.code(); }
    }

    @Test
    void completionBetweenInitialKeyReadAndCartReadIsRechecked() throws Exception {
        var request = prepare(100001L); String key = key();
        var intercept = new AtomicBoolean(true); var winner = new AtomicReference<OrderResponse>();
        doAnswer(invocation -> {
            Object initial = invocation.callRealMethod();
            if (intercept.compareAndSet(true, false)) {
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    winner.set(executor.submit(() -> orders.place(100001L, request, key)).get(5, TimeUnit.SECONDS));
                }
            }
            return initial;
        }).when(requests).completed(eq(100001L), any(OrderIntent.class));
        assertEquals(winner.get(), null);
        var replay = orders.place(100001L, request, key);
        assertEquals(winner.get(), replay); assertOneOrder();
    }

    @Test
    void orderKeyAndCartClearRollbackTogetherAfterFlushedWrites() {
        var request = prepare(100001L); var before = store.read(100001L);
        var intent = OrderIntent.from(key(), request);
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            var cart = cartRows.findWithItems(100001L).orElseThrow();
            writer.save(100001L, request.cartRevision(), OrderWriteFixtures.prepared(cart, discount), intent);
            assertEquals(1, count("orders")); assertEquals(1, count("order_requests"));
            throw new IllegalStateException("fail after flush");
        }));
        assertEquals(0, count("orders")); assertEquals(0, count("order_requests"));
        assertEquals(before, store.read(100001L));
        orders.place(100001L, request, intent.key()); assertOneOrder();
    }

    @Test
    void theSameKeyBelongsToEachMemberSeparately() {
        jdbc.update("insert into carts(member_id,revision,version) values (100002,0,0)");
        var first = prepare(100001L); var second = prepare(100002L); String key = key();
        var a = orders.place(100001L, first, key); var b = orders.place(100002L, second, key);
        assertNotEquals(a.id(), b.id()); assertEquals(2, count("orders")); assertEquals(2, count("order_requests"));
    }

    @Test
    void aStaleCartEditFailsBeforeRemoteLookup() {
        var request = prepare(100001L); long old = request.cartRevision();
        carts.change(100001L, "P001", 3, old); int calls = CATALOG.batchCalls.get();
        var error = assertThrows(ApiException.class, () -> carts.change(100001L, "P001", 4, old));
        assertEquals("CART_CHANGED", error.code()); assertEquals(calls, CATALOG.batchCalls.get());
        assertEquals(3, store.read(100001L).items().stream().filter(i -> i.code().equals("P001")).findFirst().orElseThrow().quantity());
    }

    @Test
    void versionCheckRollsBackTheLosingChildChange() throws Exception {
        prepare(100001L); var before = store.read(100001L);
        long version = jdbc.queryForObject("select version from carts where member_id=100001", Long.class);
        var loaded = new CountDownLatch(2); var release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> results = new ArrayList<>();
            for (int quantity : List.of(3,4)) results.add(executor.submit(() -> {
                try {
                    new TransactionTemplate(manager).executeWithoutResult(status -> {
                        var cart = cartRows.findWithItems(100001L).orElseThrow(); loaded.countDown(); await(release);
                        cart.change("P001", quantity); cartRows.flush();
                    });
                    return "SAVED:" + quantity;
                } catch (org.springframework.dao.OptimisticLockingFailureException expected) { return "CONFLICT"; }
            }));
            try { assertTrue(loaded.await(3, TimeUnit.SECONDS)); } finally { release.countDown(); }
            var values = List.of(results.get(0).get(5, TimeUnit.SECONDS), results.get(1).get(5, TimeUnit.SECONDS));
            assertEquals(1, values.stream().filter(v -> v.equals("CONFLICT")).count());
            int winningQuantity = Integer.parseInt(values.stream().filter(v -> v.startsWith("SAVED:")).findFirst().orElseThrow().split(":")[1]);
            assertEquals(winningQuantity, jdbc.queryForObject("select quantity from cart_items where cart_id=1 and product_code='P001'", Integer.class));
        }
        assertEquals(before.revision()+1, store.read(100001L).revision());
        assertEquals(version+1, jdbc.queryForObject("select version from carts where member_id=100001", Long.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "spaces are invalid", "한국어키는허용하지않습니다", "123456789012345!"})
    void invalidKeysAreRejectedBeforeAnyRemoteCall(String key) {
        int calls = CATALOG.batchCalls.get();
        var error = assertThrows(ApiException.class, () -> orders.place(100001L, new PlaceOrderRequest(0L, 0L), key));
        assertEquals(400, error.status().value()); assertEquals("INVALID_IDEMPOTENCY_KEY", error.code());
        assertEquals(calls, CATALOG.batchCalls.get());
    }

    @Test
    void keyLengthBoundaryAndDeterministicFingerprint() {
        var request = prepare(100001L);
        assertEquals(OrderIntent.from("a".repeat(80), request).hash(), OrderIntent.from(key(), request).hash());
        assertThrows(ApiException.class, () -> orders.place(100001L, request, "a".repeat(81)));
        orders.place(100001L, request, "a".repeat(80)); assertOneOrder();
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        var entered = new CountDownLatch(2); var release = new CountDownLatch(1);
        CATALOG.beforeBatch = () -> { entered.countDown(); await(release); };
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<T> a = executor.submit(first); Future<T> b = executor.submit(second);
            try { assertTrue(entered.await(3, TimeUnit.SECONDS)); } finally { release.countDown(); }
            return List.of(a.get(8, TimeUnit.SECONDS), b.get(8, TimeUnit.SECONDS));
        } finally { release.countDown(); CATALOG.beforeBatch = null; }
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("latch timed out"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
    }
}
