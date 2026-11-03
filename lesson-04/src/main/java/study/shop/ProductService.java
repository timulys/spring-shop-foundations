package study.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final List<Product> products = List.of(
            new Product("P001", "키보드", 30_000L, "주변기기"),
            new Product("P002", "마우스", 15_000L, "주변기기"),
            new Product("P003", "노트", 5_000L, "문구"));

    public List<Product> findAll(
            String category, String q, Long maxPrice) {
        return products.stream()
                .filter(p -> category == null || category.isBlank()
                        || p.getCategory().equals(category.strip()))
                .filter(p -> q == null || q.isBlank()
                        || p.getName().contains(q.strip()))
                .filter(p -> maxPrice == null
                        || p.getUnitPrice() <= maxPrice)
                .toList();
    }

    public Optional<Product> findByCode(String code) {
        return products.stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst();
    }
}
