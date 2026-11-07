package study.shop;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place() {
        OrderResponse order = orders.place();
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable long id) {
        return orders.cancel(id);
    }

    @GetMapping
    public OrderPageResponse list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return orders.list(page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable long id) {
        return orders.get(id);
    }
}
