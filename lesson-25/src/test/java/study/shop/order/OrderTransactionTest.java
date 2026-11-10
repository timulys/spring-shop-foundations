package study.shop.order;

import study.shop.cart.Cart;
import study.shop.cart.CartRepository;
import study.shop.common.ApiException;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

// 테스트 자체에는 @Transactional을 붙이지 않습니다.
// 서비스 프록시가 종료된 다음 별도 조회로 실제 커밋/롤백 결과를 확인합니다.
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.show-sql=true")
@Import(OrderTransactionTest.ProbeConfiguration.class)
class OrderTransactionTest {
    @Autowired OrderService service;
    @Autowired TransactionProbe probe;
    @Autowired JdbcTemplate jdbc;
    @Autowired CartRepository carts;
    @Autowired PlatformTransactionManager transactions;
    private record CartRow(String code, int quantity) {}
    private java.util.List<CartRow> originalCart;
    private final java.util.Set<Long> createdIds = new java.util.HashSet<>();

    @BeforeEach
    void prepare() {
        originalCart = jdbc.query("SELECT product_code, quantity FROM cart_items WHERE cart_id=1",
                (rs, row) -> new CartRow(rs.getString(1), rs.getInt(2)));
        probe.reset();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Cart cart = carts.findById(1L).orElseThrow();
            cart.clear();
        });
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Cart cart = carts.findById(1L).orElseThrow();
            cart.add("P001", 2);
            cart.add("P002", 1);
        });
    }

    @AfterEach
    void cleanOnlyThisTestData() {
        // 실행 전 주문은 보존하고, 이 테스트가 만든 주문만 정리합니다.
        if (probe.lastOrderId() != null) createdIds.add(probe.lastOrderId());
        for (long id : createdIds) {
            jdbc.update("DELETE FROM order_items WHERE order_id = ?", id);
            jdbc.update("DELETE FROM orders WHERE id = ?", id);
        }
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                carts.findById(1L).orElseThrow().clear());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Cart cart = carts.findById(1L).orElseThrow();
            for (CartRow row : originalCart) cart.add(row.code(), row.quantity());
        });
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    @Test
    void commitsOrderItemsAndCartClearTogether() {
        long ordersBefore = count("orders");
        long itemsBefore = count("order_items");
        OrderResponse order = service.place(OrderTestRequests.current(service));
        createdIds.add(order.id());
        assertEquals(70_000L, order.total());
        assertEquals(ordersBefore + 1, count("orders"));
        assertEquals(itemsBefore + 2, count("order_items"));
        assertEquals(0, count("cart_items"));
        System.out.println("COMMIT: 주문 +1, 주문 항목 +2, 장바구니 항목 0");
    }

    @Test
    void rollsBackEvenAfterSqlWasFlushed() {
        long ordersBefore = count("orders");
        long itemsBefore = count("order_items");
        assertThrows(IllegalStateException.class, () -> probe.failWithRuntimeException());
        assertEquals(ordersBefore, count("orders"));
        assertEquals(itemsBefore, count("order_items"));
        assertEquals(2, count("cart_items"));
        assertEquals(2, jdbc.queryForObject(
                "SELECT quantity FROM cart_items WHERE product_code='P001'", Integer.class));
        System.out.println("ROLLBACK: 주문 +0, 주문 항목 +0, 장바구니 항목 2 유지");
    }

    @Test
    void checkedExceptionCommitsByDefault() {
        long ordersBefore = count("orders");
        assertThrows(CheckedFailure.class, () -> probe.failWithCheckedException());
        assertEquals(ordersBefore + 1, count("orders"));
        assertEquals(0, count("cart_items"));
        System.out.println("checked 기본 규칙: 예외가 나도 커밋, 장바구니 항목 0");
    }

    @Test
    void rollbackForIncludesCheckedException() {
        long ordersBefore = count("orders");
        long itemsBefore = count("order_items");
        assertThrows(CheckedFailure.class, () -> probe.failWithCheckedRollbackRule());
        assertEquals(ordersBefore, count("orders"));
        assertEquals(itemsBefore, count("order_items"));
        assertEquals(2, count("cart_items"));
        System.out.println("rollbackFor 적용: 주문 +0, 장바구니 항목 2 유지");
    }

    @Test
    void rejectsASecondOrderAfterCartWasCleared() {
        createdIds.add(service.place(OrderTestRequests.current(service)).id());
        long before = count("orders");
        ApiException error = assertThrows(ApiException.class, () -> service.place(new PlaceOrderRequest(0L, 0L)));
        assertEquals("EMPTY_CART", error.code());
        assertEquals(before, count("orders"));
        assertEquals(0, count("cart_items"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean
        TransactionProbe transactionProbe(OrderService orders, EntityManager em) {
            return new TransactionProbe(orders, em);
        }
    }

    static class CheckedFailure extends Exception {
    }

    // 테스트에서만 사용하는 빈입니다. HTTP API에는 실패 기능을 추가하지 않습니다.
    public static class TransactionProbe {
        private final OrderService orders;
        private final EntityManager em;
        private Long lastOrderId;

        TransactionProbe(OrderService orders, EntityManager em) {
            this.orders = orders;
            this.em = em;
        }

        public void reset() { lastOrderId = null; }
        public Long lastOrderId() { return lastOrderId; }

        private void placeAndFlush() {
            lastOrderId = orders.place(OrderTestRequests.current(orders)).id();
            em.flush();
            long remaining = ((Number) em.createNativeQuery(
                    "SELECT count(*) FROM cart_items").getSingleResult()).longValue();
            assertEquals(0L, remaining);
            System.out.println("flush 직후 같은 트랜잭션에서 장바구니 항목 0 확인");
        }

        @Transactional
        public void failWithRuntimeException() {
            placeAndFlush();
            throw new IllegalStateException("주문 저장과 장바구니 삭제 후 의도한 실패");
        }

        @Transactional
        public void failWithCheckedException() throws CheckedFailure {
            placeAndFlush();
            throw new CheckedFailure();
        }

        @Transactional(rollbackFor = CheckedFailure.class)
        public void failWithCheckedRollbackRule() throws CheckedFailure {
            placeAndFlush();
            throw new CheckedFailure();
        }
    }
}
