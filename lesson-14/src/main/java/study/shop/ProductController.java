package study.shop;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public List<ProductResponse> products(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "maxPrice", required = false) Long maxPrice) {
        if (q != null && q.length() > 40) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "검색어는 40자까지 입력할 수 있습니다.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "최대 가격은 0원 이상이어야 합니다.");
        }
        return productService.findAll(category, q, maxPrice).stream()
                .map(ProductResponse::new)
                .toList();
    }

    @GetMapping("/api/products/{code}")
    public ProductResponse product(@PathVariable("code") String code) {
        Product product = productService.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        return new ProductResponse(product);
    }
}
