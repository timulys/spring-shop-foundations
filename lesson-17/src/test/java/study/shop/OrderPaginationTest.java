package study.shop;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderPaginationTest {
    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    private final List<Long> ids = new ArrayList<>();
    private long existingCount;
    private Instant fixedTime;
    private TransactionTemplate tx;

    @BeforeEach
    void prepare() {
        tx = new TransactionTemplate(manager);
        existingCount = orders.count();
        Timestamp latest = jdbc.queryForObject("SELECT max(created_at) FROM orders", Timestamp.class);
        fixedTime = latest == null ? Instant.now() : latest.toInstant().plusSeconds(1);
        for (int i = 0; i < 5; i++) createAt(fixedTime);
    }

    private long createAt(Instant time) {
        long id = tx.execute(status -> {
            ShopOrder order = new ShopOrder();
            order.addItem(products.findById("P001").orElseThrow(), 2);
            order.confirm();
            return orders.save(order).getId();
        });
        ids.add(id);
        jdbc.update("UPDATE orders SET created_at = ? WHERE id = ?", Timestamp.from(time), id);
        return id;
    }

    @AfterEach
    void cleanOnlyFixtureOrders() {
        for (long id : ids) {
            jdbc.update("DELETE FROM order_items WHERE order_id = ?", id);
            jdbc.update("DELETE FROM orders WHERE id = ?", id);
        }
    }

    private List<Long> pageIds(int number, int size) {
        return service.list(number, size).content().stream()
                .map(OrderSummaryResponse::id).toList();
    }

    @Test
    void tiesUseIdAndPagesDoNotOverlapWhileDataIsUnchanged() {
        var expected = new ArrayList<>(ids);
        Collections.reverse(expected);
        assertEquals(expected.subList(0, 2), pageIds(0, 2));
        assertEquals(expected.subList(2, 4), pageIds(1, 2));
        assertEquals(expected.get(4), pageIds(2, 2).getFirst());
        assertEquals(4, java.util.stream.Stream.concat(
                pageIds(0, 2).stream(), pageIds(1, 2).stream()).distinct().count());
        System.out.println("고정 데이터 정렬: 같은 생성 시각은 id 내림차순, 앞 두 페이지 중복 0");
    }

    @Test
    void creationTimeTakesPriorityOverId() {
        long lowestId = ids.getFirst();
        jdbc.update("UPDATE orders SET created_at = ? WHERE id = ?",
                Timestamp.from(fixedTime.plusSeconds(10)), lowestId);
        assertEquals(lowestId, pageIds(0, 2).getFirst());
    }

    @Test
    void returnsPageMetadataAndSummaryFields() {
        OrderPageResponse first = service.list(0, 2);
        assertEquals(0, first.number());
        assertEquals(2, first.size());
        assertEquals(existingCount + 5, first.totalElements());
        assertEquals((existingCount + 6) / 2, first.totalPages());
        assertTrue(first.hasNext());
        assertEquals(55_000L, first.content().getFirst().total());
        assertEquals(OrderStatus.CONFIRMED, first.content().getFirst().status());
    }

    @Test
    void acceptsMinimumAndMaximumPageSizes() {
        assertEquals(1, service.list(0, 1).content().size());
        assertEquals(50, service.list(0, 50).size());
        assertTrue(service.list(0, 50).content().size() <= 50);
    }

    @Test
    void pageBeyondEndReturnsEmptyContentAndKeepsTotal() {
        int afterLast = Math.toIntExact((existingCount + 5) / 2 + 1);
        OrderPageResponse empty = service.list(afterLast, 2);
        assertTrue(empty.content().isEmpty());
        assertFalse(empty.hasNext());
        assertEquals(existingCount + 5, empty.totalElements());
        System.out.println("마지막 이후 페이지: content=[], hasNext=false, 전체 개수 유지");
    }

    @Test
    void rejectsInvalidPageSizeAndOffset() {
        for (int[] args : List.of(new int[]{-1, 2}, new int[]{0, 0},
                new int[]{0, 51}, new int[]{Integer.MAX_VALUE, 50})) {
            ApiException ex = assertThrows(ApiException.class,
                    () -> service.list(args[0], args[1]));
            assertEquals(400, ex.status().value());
            assertEquals("INVALID_PAGE", ex.code());
        }
    }

    @Test
    void insertionBetweenRequestsCanMoveOffsetBoundary() {
        List<Long> before = pageIds(0, 2);
        long inserted = createAt(fixedTime.plusSeconds(10));
        assertEquals(inserted, pageIds(0, 2).getFirst());
        assertEquals(before.get(1), pageIds(1, 2).getFirst());
        System.out.println("새 주문 삽입 뒤: 이전 첫 페이지의 마지막 주문이 다음 페이지에 다시 나타남");
    }

    @Test
    void cancelledOrdersRemainInTheList() {
        long id = ids.getLast();
        service.cancel(id);
        OrderSummaryResponse row = service.list(0, 2).content().getFirst();
        assertEquals(id, row.id());
        assertEquals(OrderStatus.CANCELLED, row.status());
        assertEquals(55_000L, row.total());
    }
}
