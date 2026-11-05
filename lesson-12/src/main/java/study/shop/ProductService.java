package study.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll(
            String category, String q, Long maxPrice) {
        return productRepository.findAll(Sort.by("code")).stream()
                .filter(p -> category == null || category.isBlank()
                        || p.getCategory().equals(category.strip()))
                .filter(p -> q == null || q.isBlank()
                        || p.getName().contains(q.strip()))
                .filter(p -> maxPrice == null
                        || p.getUnitPrice() <= maxPrice)
                .toList();
    }

    public Optional<Product> findByCode(String code) {
        return productRepository.findById(code);
    }
}
