package study.shop;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.show-sql=true")
@Transactional
class OrderSnapshotTest {
    @Autowired EntityManager em;
    @Autowired CartRepository carts;
    @Autowired OrderRepository orders;
    @Autowired OrderService service;

    @BeforeEach
    void prepareCart() {
        Cart cart = carts.findById(1L).orElseThrow();
        for (CartItem item : cart.getItems()) cart.remove(item.getProductCode());
        em.flush();
    }

    private OrderResponse placeSample() {
        Cart cart = carts.findById(1L).orElseThrow();
        cart.add("P001", 2);
        cart.add("P002", 1);
        return service.place();
    }

    @Test
    void preservesPurchasedNamePriceAndQuantityAfterCatalogChange() {
        OrderResponse original = placeSample();
        assertEquals(75_000L, original.subtotal());
        assertEquals(5_000L, original.discount());
        assertEquals(0L, original.shippingFee());
        assertEquals(70_000L, original.total());
        em.flush();
        em.createNativeQuery("UPDATE products SET name='새 키보드', unit_price_won=40000 WHERE code='P001'")
                .executeUpdate();
        em.clear();
        OrderResponse reloaded = service.get(original.id());
        assertEquals(original, reloaded);
        assertEquals("키보드", reloaded.items().getFirst().productName());
        assertEquals(30_000L, reloaded.items().getFirst().unitPrice());
        assertEquals(2, reloaded.items().getFirst().quantity());
        assertEquals("새 키보드", em.find(Product.class, "P001").getName());
        System.out.println("현재 상품: 새 키보드 40000원 / 이전 주문: 키보드 30000원, 총액 70000원 유지");
    }

    @Test
    void storesStateAsNameAndKeepsTheCartAtThisLesson() {
        OrderResponse order = placeSample();
        em.flush();
        em.clear();
        assertEquals("CONFIRMED", em.createNativeQuery("SELECT status FROM orders WHERE id=:id")
                .setParameter("id", order.id()).getSingleResult());
        assertEquals("SIMULATED", service.get(order.id()).paymentMode());
        assertEquals(2, carts.findById(1L).orElseThrow().getItems().size());
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT count(*) FROM order_items WHERE order_id=:id")
                .setParameter("id", order.id()).getSingleResult()).longValue());
    }

    @Test
    void readsAnOldOrderWithoutTheCurrentProductRow() {
        OrderResponse order = placeSample();
        em.flush();
        em.createNativeQuery("DELETE FROM products WHERE code='P001'").executeUpdate();
        em.clear();
        assertEquals(order, service.get(order.id()));
    }

    @Test
    void rejectsAnEmptyCartBeforeSavingAnOrder() {
        long before = orders.count();
        ApiException error = assertThrows(ApiException.class, () -> service.place());
        assertEquals("EMPTY_CART", error.code());
        assertEquals(before, orders.count());
    }
}
