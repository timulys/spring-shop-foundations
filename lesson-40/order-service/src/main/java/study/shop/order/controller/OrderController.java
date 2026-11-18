package study.shop.order.controller;

import study.shop.order.dto.OrderPageResponse;
import study.shop.order.dto.OrderPreviewResponse;
import study.shop.order.dto.OrderResponse;
import study.shop.order.dto.PlaceOrderRequest;
import study.shop.order.service.OrderService;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import study.shop.member.security.ShopPrincipal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/preview")
    public OrderPreviewResponse preview(@AuthenticationPrincipal ShopPrincipal principal) {
        return orders.preview(principal.memberId());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal ShopPrincipal principal, @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = orders.place(principal.memberId(), request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal ShopPrincipal principal, @PathVariable long id) {
        return orders.cancel(principal.memberId(), id);
    }

    @GetMapping
    public OrderPageResponse list(@AuthenticationPrincipal ShopPrincipal principal, @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return orders.list(principal.memberId(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal ShopPrincipal principal, @PathVariable long id) {
        return orders.get(principal.memberId(), id);
    }
}
