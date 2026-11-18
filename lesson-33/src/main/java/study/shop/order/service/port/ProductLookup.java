package study.shop.order.service.port;

import study.shop.order.domain.ProductSnapshot;
import java.util.Optional;

@FunctionalInterface
public interface ProductLookup {
    Optional<ProductSnapshot> findByCode(String code);
}
