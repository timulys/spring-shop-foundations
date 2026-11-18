package study.shop.order.infrastructure;

import study.shop.catalog.repository.ProductRepository;
import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;
import study.shop.order.service.port.ProductLookup;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class DatabaseProductLookup implements ProductLookup {
    private final ProductRepository products;

    public DatabaseProductLookup(ProductRepository products) {
        this.products = products;
    }

    @Override
    public Optional<ProductSnapshot> findByCode(String code) {
        return products.findById(code).map(product -> new ProductSnapshot(
                product.getCode(), product.getName(), Money.ofWon(product.getUnitPrice())));
    }
}
