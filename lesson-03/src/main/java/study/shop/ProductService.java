package study.shop;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final List<Product> products = List.of(
            new Product("P001", "키보드", 30_000L, "주변기기"),
            new Product("P002", "마우스", 15_000L, "주변기기"),
            new Product("P003", "노트", 5_000L, "문구"));

    public List<Product> findAll() {
        return products;
    }
}
