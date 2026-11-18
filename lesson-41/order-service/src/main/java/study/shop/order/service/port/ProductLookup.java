package study.shop.order.service.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import study.shop.common.ApiException;
import study.shop.order.domain.ProductSnapshot;

@FunctionalInterface
public interface ProductLookup {
    Map<String, ProductSnapshot> findByCodes(List<String> codes);

    default Map<String, ProductSnapshot> requireAll(List<String> codes) {
        Map<String, ProductSnapshot> result = findByCodes(codes);
        if (!result.keySet().containsAll(codes)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                    "상품을 찾을 수 없습니다.");
        }
        return result;
    }

    default Optional<ProductSnapshot> findByCode(String code) {
        return Optional.ofNullable(findByCodes(List.of(code)).get(code));
    }
}
