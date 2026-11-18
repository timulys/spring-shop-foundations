package study.shop.common;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;

/** Test-only HTTP peer. No extra application or catalog DB is required to run order tests. */
public abstract class CatalogServerTestBase {
    public static final CatalogStub CATALOG = new CatalogStub();

    @DynamicPropertySource
    static void catalogAddress(DynamicPropertyRegistry registry) {
        registry.add("catalog.base-url", CATALOG::baseUrl);
    }

    @BeforeEach
    void resetRemoteCatalog() { CATALOG.reset(); }

    // For persistence fixtures only; production behavior uses the real HTTP adapter.
    public static ProductSnapshot catalogSnapshot(String code) {
        var p = CATALOG.products.get(code);
        return new ProductSnapshot(p.code(), p.name(), Money.ofWon(p.unitPrice()));
    }
}
