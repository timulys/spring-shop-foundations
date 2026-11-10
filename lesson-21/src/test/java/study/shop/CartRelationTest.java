package study.shop;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.show-sql=true")
@Transactional
class CartRelationTest {
    @Autowired EntityManager entityManager;
    @Autowired CartRepository carts;
    @Autowired ProductRepository products;

    @BeforeEach
    void prepareEmptyLearningCart() {
        Cart cart = carts.findById(1L).orElseThrow();
        for (CartItem item : cart.getItems()) {
            cart.remove(item.getProductCode());
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void persistsChildWithParentAndForeignKey() {
        Cart cart = new Cart(2L);
        cart.add("P001", 1);
        assertSame(cart, cart.getItems().getFirst().getCart());
        entityManager.persist(cart);
        entityManager.flush();
        entityManager.clear();
        Cart reloaded = carts.findById(2L).orElseThrow();
        assertEquals(1, reloaded.getItems().size());
        assertEquals(2L, reloaded.getItems().getFirst().getCart().getId());
        assertEquals("P001", reloaded.getItems().getFirst().getProductCode());
    }

    @Test
    void combinesOnlyTheSameProductInTheSameCart() {
        Cart first = carts.findById(1L).orElseThrow();
        first.add("P001", 1);
        first.add("P001", 1);
        Cart other = new Cart(2L);
        other.add("P001", 3);
        entityManager.persist(other);
        entityManager.flush();
        entityManager.clear();
        assertEquals(1, carts.findById(1L).orElseThrow().getItems().size());
        assertEquals(2, carts.findById(1L).orElseThrow().getItems().getFirst().getQuantity());
        assertEquals(3, carts.findById(2L).orElseThrow().getItems().getFirst().getQuantity());
    }

    @Test
    void removesOnlyTheSelectedOrphan() {
        Cart first = carts.findById(1L).orElseThrow();
        first.add("P001", 1);
        first.add("P002", 1);
        Cart other = new Cart(2L);
        other.add("P001", 3);
        entityManager.persist(other);
        entityManager.flush();
        Long removedId = first.getItems().getFirst().getId();
        first.remove("P001");
        entityManager.flush();
        entityManager.clear();
        assertNull(entityManager.find(CartItem.class, removedId));
        assertEquals("P002", carts.findById(1L).orElseThrow().getItems().getFirst().getProductCode());
        assertEquals(3, carts.findById(2L).orElseThrow().getItems().getFirst().getQuantity());
        assertEquals(3, products.count());
        System.out.println("선택한 항목 삭제: 다른 장바구니와 상품 3개 유지");
    }

    @Test
    void rejectsOneMoreWithoutChangingNinetyNine() {
        Cart cart = carts.findById(1L).orElseThrow();
        cart.add("P001", 99);
        assertThrows(ApiException.class, () -> cart.add("P001", 1));
        assertEquals(99, cart.getItems().getFirst().getQuantity());
        entityManager.flush();
        entityManager.clear();
        assertEquals(99, carts.findById(1L).orElseThrow().getItems().getFirst().getQuantity());
    }

    @Test
    void parentRemovalDoesNotReachProductsOrAnotherCart() {
        Cart other = new Cart(2L);
        other.add("P001", 1);
        entityManager.persist(other);
        entityManager.flush();
        Long itemId = other.getItems().getFirst().getId();
        entityManager.remove(other);
        entityManager.flush();
        entityManager.clear();
        assertNull(entityManager.find(Cart.class, 2L));
        assertNull(entityManager.find(CartItem.class, itemId));
        assertTrue(carts.existsById(1L));
        assertEquals(3, products.count());
    }
}
