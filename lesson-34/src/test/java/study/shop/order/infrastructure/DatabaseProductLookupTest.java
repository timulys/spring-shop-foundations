package study.shop.order.infrastructure;

import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DatabaseProductLookupTest {
    @Autowired DatabaseProductLookup products;

    @Test
    void databaseEntityBecomesAnOrderSnapshot() {
        assertThat(products.findByCode("P001")).contains(
                new ProductSnapshot("P001", "키보드", Money.ofWon(30_000)));
    }

    @Test
    void absentProductRemainsEmpty() {
        assertThat(products.findByCode("MISSING")).isEmpty();
    }
}
