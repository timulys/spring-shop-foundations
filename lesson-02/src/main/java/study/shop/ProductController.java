package study.shop;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    private final List<Product> products = List.of(
            new Product("P001", "키보드", 30_000L, "주변기기"),
            new Product("P002", "마우스", 15_000L, "주변기기"),
            new Product("P003", "노트", 5_000L, "문구"));

    @GetMapping("/api/products")
    public List<ProductResponse> products() {
        return products.stream()
                .map(ProductResponse::new)
                .toList();
    }
}
