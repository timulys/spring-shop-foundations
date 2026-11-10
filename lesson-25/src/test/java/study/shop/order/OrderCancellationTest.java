package study.shop.order;

import study.shop.catalog.ProductRepository;
import study.shop.common.ApiException;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

// 테스트에는 @Transactional을 붙이지 않습니다. 실제 커밋 뒤 별도 조회합니다.
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.show-sql=true")
class OrderCancellationTest {
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    private long id;
    private TransactionTemplate tx;

    @BeforeEach
    void prepare() {
        tx = new TransactionTemplate(manager);
        id = tx.execute(status -> {
            ShopOrder order = new ShopOrder();
            order.addItem(products.findById("P001").orElseThrow(), 2);
            order.addItem(products.findById("P002").orElseThrow(), 1);
            order.confirm();
            return orders.save(order).getId();
        });
    }

    @AfterEach
    void cleanOnlyThisOrder() {
        jdbc.update("DELETE FROM order_items WHERE order_id = ?", id);
        jdbc.update("DELETE FROM orders WHERE id = ?", id);
    }

    private String storedStatus() {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, id);
    }

    @Test
    void commitsCancellationWithoutCallingSaveAgain() {
        OrderResponse before = service.get(id);
        OrderResponse cancelled = service.cancel(id);
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals("CANCELLED", storedStatus());
        assertEquals(before.items(), cancelled.items());
        assertEquals(before.total(), cancelled.total());
        assertEquals(70_000L, cancelled.total());
        assertEquals(before.createdAt(), cancelled.createdAt());
        System.out.println("정상 취소: CANCELLED, 주문 항목 2개와 총액 70000원 유지");
    }

    @Test
    void rejectsRepeatedCancellation() {
        service.cancel(id);
        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(id));
        assertEquals(409, ex.status().value());
        assertEquals("ORDER_STATE_CONFLICT", ex.code());
        assertEquals("CANCELLED", storedStatus());
    }

    @Test
    void rejectsDraftCancellation() {
        ApiException ex = assertThrows(ApiException.class, () -> new ShopOrder().cancel());
        assertEquals(409, ex.status().value());
    }

    @Test
    void rejectsMissingOrder() {
        ApiException ex = assertThrows(ApiException.class, () -> service.cancel(Long.MAX_VALUE));
        assertEquals(404, ex.status().value());
        assertEquals("ORDER_NOT_FOUND", ex.code());
    }

    @Test
    void managedChangeIsDetectedAndCommitted() {
        tx.executeWithoutResult(status -> {
            ShopOrder order = em.find(ShopOrder.class, id);
            assertTrue(em.contains(order));
            order.cancel();
            // save 없이도 트랜잭션 커밋 전 변경 내용이 반영됩니다.
        });
        assertEquals("CANCELLED", storedStatus());
        System.out.println("관리 상태: contains=true, save 없이 CANCELLED 커밋");
    }

    @Test
    void detachedChangeIsNotTracked() {
        tx.executeWithoutResult(status -> {
            ShopOrder order = em.find(ShopOrder.class, id);
            em.detach(order);
            assertFalse(em.contains(order));
            order.cancel();
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            em.flush();
        });
        assertEquals("CONFIRMED", storedStatus());
        System.out.println("분리 상태: 객체 CANCELLED, DB CONFIRMED 유지");
    }

    @Test
    void flushThenFailureRollsBackCancellation() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            ShopOrder order = em.find(ShopOrder.class, id);
            order.cancel();
            em.flush();
            String inTransaction = (String) em.createNativeQuery(
                    "SELECT status FROM orders WHERE id = :id")
                    .setParameter("id", id).getSingleResult();
            assertEquals("CANCELLED", inTransaction);
            System.out.println("flush 직후 같은 트랜잭션: CANCELLED");
            throw new IllegalStateException("flush 뒤 의도한 실패");
        }));
        assertEquals("CONFIRMED", storedStatus());
        assertEquals(70_000L, service.get(id).total());
        System.out.println("롤백 뒤 별도 조회: CONFIRMED, 총액 70000원 유지");
    }

    @Test
    void concurrentRequestsProduceOneSuccessAndOneConflict() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> task = () -> {
                start.await();
                try { service.cancel(id); return 200; }
                catch (ApiException ex) { return ex.status().value(); }
            };
            Future<Integer> first = pool.submit(task);
            Future<Integer> second = pool.submit(task);
            start.countDown();
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
                    .stream().sorted().toList();
            assertEquals(List.of(200, 409), results);
            assertEquals("CANCELLED", storedStatus());
        }
    }
}
