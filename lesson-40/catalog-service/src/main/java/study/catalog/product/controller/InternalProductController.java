package study.catalog.product.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import study.catalog.product.dto.ProductLookupRequest;
import study.catalog.product.dto.ProductLookupResponse;
import study.catalog.product.service.ProductService;
import tools.jackson.databind.JsonNode;

@RestController
public class InternalProductController {
    private final ProductService products;

    public InternalProductController(ProductService products) {
        this.products = products;
    }

    @PostMapping("/internal/products/lookup")
    public ProductLookupResponse lookup(@RequestBody JsonNode body) {
        var request = ProductLookupRequest.fromJson(body);
        return new ProductLookupResponse(products.findByCodes(request.productCodes()).stream()
                .map(ProductLookupResponse.Item::from).toList());
    }
}
