package study.shop.cart.controller;

import study.shop.cart.dto.AddCartItemRequest;
import study.shop.cart.dto.CartResponse;
import study.shop.cart.dto.ChangeQuantityRequest;
import study.shop.cart.service.CartService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import study.shop.member.security.ShopPrincipal;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse cart(@AuthenticationPrincipal ShopPrincipal principal) {
        return cartService.get(principal.memberId());
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal ShopPrincipal principal, @Valid @RequestBody AddCartItemRequest request,
            @RequestHeader(name="X-Cart-Revision", required=false) Long revision) {
        return cartService.add(principal.memberId(), request.productCode(), request.quantity(), revision);
    }

    @PatchMapping("/items/{code}")
    public CartResponse change(@AuthenticationPrincipal ShopPrincipal principal, @PathVariable("code") String code,
            @Valid @RequestBody ChangeQuantityRequest request,
            @RequestHeader(name="X-Cart-Revision", required=false) Long revision) {
        return cartService.change(principal.memberId(), code, request.quantity(), revision);
    }

    @DeleteMapping("/items/{code}")
    public CartResponse remove(@AuthenticationPrincipal ShopPrincipal principal, @PathVariable("code") String code,
            @RequestHeader(name="X-Cart-Revision", required=false) Long revision) {
        return cartService.remove(principal.memberId(), code, revision);
    }
}
