package study.shop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=study.shop.SqlCapture"
})
class OrderQueryTest {
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager manager;
    private final List<Long> ids = new ArrayList<>();
    private TransactionTemplate tx;

    @BeforeEach
    void prepareThreeOrdersWithTwoItems() {
        tx = new TransactionTemplate(manager);
        for (int i = 0; i < 3; i++) {
            ids.add(tx.execute(status -> {
                ShopOrder order = new ShopOrder();
                order.addItem(products.findById("P001").orElseThrow(), 2);
                order.addItem(products.findById("P002").orElseThrow(), 1);
                order.confirm();
                return orders.save(order).getId();
            }));
        }
        SqlCapture.clear();
    }

    @AfterEach
    void removeOnlyFixtureOrders() {
        for (long id : ids) {
            jdbc.update("DELETE FROM order_items WHERE order_id = ?", id);
            jdbc.update("DELETE FROM orders WHERE id = ?", id);
        }
        SqlCapture.finish();
    }

    private static void printSql(String label) {
        List<String> sql = SqlCapture.selects();
        System.out.println(label + " SELECT " + sql.size() + "회");
        for (int i = 0; i < sql.size(); i++) {
            System.out.println((i + 1) + ". " + sql.get(i));
        }
    }

    @Test
    void detailFetchKeepsResponseAndUsesOneSelect() {
        OrderResponse before = tx.execute(status ->
                OrderResponse.from(orders.findById(ids.getFirst()).orElseThrow()));
        assertEquals(2, SqlCapture.selects().size());
        printSql("기존 상세: 주문 조회 + 항목 조회");
        SqlCapture.clear();
        OrderResponse after = service.get(ids.getFirst());
        assertEquals(before, after);
        assertEquals(2, after.items().size());
        assertEquals(70_000L, after.total());
        assertEquals(1, SqlCapture.selects().size());
        assertTrue(SqlCapture.selects().getFirst().contains("join order_items"));
        printSql("개선 상세: 같은 응답, fetch join");
    }

    @Test
    void threeLazyCollectionsProduceFourSelectsInFreshContext() {
        List<OrderResponse> before = tx.execute(status -> orders.findAllById(ids)
                .stream().map(OrderResponse::from)
                .sorted(Comparator.comparingLong(OrderResponse::id)).toList());
        assertEquals(4, SqlCapture.selects().size());
        printSql("주문 3개를 조회하고 각 항목에 접근");
        SqlCapture.clear();
        List<OrderResponse> after = tx.execute(status -> em.createQuery("""
                select o from ShopOrder o left join fetch o.items where o.id in :ids
                """, ShopOrder.class).setParameter("ids", ids).getResultList()
                .stream().map(OrderResponse::from)
                .sorted(Comparator.comparingLong(OrderResponse::id)).toList());
        assertEquals(before, after);
        assertEquals(3, after.size());
        assertEquals(1, SqlCapture.selects().size());
        printSql("지정한 주문 3개와 항목을 함께 조회");
    }

    @Test
    void summaryProjectionKeepsPageAndSelectsOnlyFourFields() {
        var pageable = PageRequest.of(0, 2,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        OrderPageResponse before = tx.execute(status -> OrderPageResponse.from(
                orders.findAll(pageable).map(OrderSummaryResponse::from)));
        printSql("기존 목록: 엔티티 조회 후 DTO 변환");
        String entitySql = SqlCapture.selects().getFirst();
        assertTrue(entitySql.contains("discount_won"));
        SqlCapture.clear();
        OrderPageResponse after = service.list(0, 2);
        assertEquals(before, after);
        List<String> sql = SqlCapture.selects();
        assertEquals(2, sql.size());
        String selectColumns = sql.getFirst().split(" from ")[0];
        assertEquals(4, selectColumns.split(",").length);
        assertFalse(selectColumns.contains("discount_won"));
        assertFalse(sql.stream().anyMatch(s -> s.contains("order_items")));
        assertTrue(sql.get(1).contains("count("));
        printSql("개선 목록: 요약 4개 필드 + 전체 개수");
    }

    @Test
    void uninitializedCollectionNeedsAnOpenContext() {
        ShopOrder detached = tx.execute(status -> {
            ShopOrder order = orders.findById(ids.getFirst()).orElseThrow();
            assertFalse(emf.getPersistenceUnitUtil().isLoaded(order, "items"));
            return order;
        });
        assertThrows(LazyInitializationException.class, detached::getItems);
        System.out.println("미초기화 항목: 컨텍스트가 닫힌 뒤 접근하면 LazyInitializationException");
        OrderResponse ready = service.get(ids.getFirst());
        assertEquals(2, ready.items().size());
        System.out.println("서비스 안에서 완성한 DTO: 반환 뒤에도 항목 2개 사용 가능");
    }

    @Test
    void fetchedCollectionRemainsAvailableAfterContextCloses() {
        ShopOrder detached = tx.execute(status -> {
            ShopOrder order = orders.findDetailById(ids.getFirst()).orElseThrow();
            assertTrue(emf.getPersistenceUnitUtil().isLoaded(order, "items"));
            return order;
        });
        assertEquals(2, detached.getItems().size());
        assertEquals(1, SqlCapture.selects().size());
    }

    @Test
    void historicalDetailUsesSnapshotsWithoutProductSelect() {
        OrderResponse before = service.get(ids.getFirst());
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE products SET name = ?, unit_price_won = ? WHERE code = ?",
                    "이름 변경 실습", 99_000L, "P001");
            SqlCapture.clear();
            OrderResponse after = service.get(ids.getFirst());
            assertEquals(before, after);
            assertEquals(1, SqlCapture.selects().size());
            assertFalse(SqlCapture.selects().stream().anyMatch(sql -> sql.contains("products")));
            printSql("현재 상품 변경 뒤에도 주문 스냅샷 유지");
            status.setRollbackOnly();
        });
    }

    @Test
    void missingDetailKeepsNotFoundContract() {
        ApiException error = assertThrows(ApiException.class, () -> service.get(-1));
        assertEquals(404, error.status().value());
        assertEquals("ORDER_NOT_FOUND", error.code());
    }
}
