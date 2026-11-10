package study.shop.cart;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse cart() {
        return cartService.get();
    }

    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.add(request.productCode(), request.quantity());
    }

    @PatchMapping("/items/{code}")
    public CartResponse change(@PathVariable("code") String code,
            @Valid @RequestBody ChangeQuantityRequest request) {
        return cartService.change(code, request.quantity());
    }

    @DeleteMapping("/items/{code}")
    public CartResponse remove(@PathVariable("code") String code) {
        return cartService.remove(code);
    }
}
