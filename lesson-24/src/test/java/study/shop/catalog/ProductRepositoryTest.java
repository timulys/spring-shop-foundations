package study.shop.catalog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.show-sql=true")
@Transactional
class ProductRepositoryTest {
    @Autowired
    ProductRepository productRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void findsExistingAndMissingProducts() {
        Product keyboard = productRepository.findById("P001").orElseThrow();
        assertEquals("키보드", keyboard.getName());
        assertTrue(productRepository.findById("P999").isEmpty());
    }

    @Test
    void savesReadsAndChangesOneProduct() {
        Product input = new Product("P004", "연필", 1_000L, "문구");
        Product saved = productRepository.save(input);
        assertNotSame(input, saved);
        assertTrue(entityManager.contains(saved));
        productRepository.flush();
        entityManager.clear();

        Product loaded = productRepository.findById("P004").orElseThrow();
        assertEquals("연필", loaded.getName());
        assertEquals(1_000L, loaded.getUnitPrice());
        System.out.println("P004 저장 후 조회: " + loaded.getUnitPrice());

        Product changed = productRepository.save(
                new Product("P004", "연필", 1_200L, "문구"));
        assertEquals(1_200L, changed.getUnitPrice());
        productRepository.flush();
        entityManager.clear();

        Product reloaded = productRepository.findById("P004").orElseThrow();
        assertEquals(1_200L, reloaded.getUnitPrice());
        assertEquals(1L, productRepository.findAll().stream()
                .filter(p -> p.getCode().equals("P004")).count());
        System.out.println("P004 변경 후 조회: " + reloaded.getUnitPrice());
    }
}
