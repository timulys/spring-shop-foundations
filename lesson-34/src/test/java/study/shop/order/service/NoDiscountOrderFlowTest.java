package study.shop.order.service;

import study.shop.cart.domain.Cart;
import study.shop.cart.repository.CartRepository;
import study.shop.order.domain.DiscountPolicy;
import study.shop.order.domain.NoDiscountPolicy;
import study.shop.order.domain.OrderStatus;
import study.shop.order.dto.PlaceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@org.springframework.test.context.jdbc.Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Import(NoDiscountOrderFlowTest.NoDiscountConfiguration.class)
class NoDiscountOrderFlowTest {
    @Autowired OrderService orders;
    @Autowired CartRepository carts;

    @BeforeEach
    void prepareCart() {
        Cart cart = carts.findForUpdate(100001L).orElseThrow();
        cart.clear();
        cart.add("P001", 2);
        cart.add("P002", 1);
    }

    @Test
    void sameServiceFlowUsesNoDiscountForPreviewAndPlacement() {
        var preview = orders.preview(100001L);
        assertThat(preview.total()).isEqualTo(75_000);
        var placed = orders.place(100001L, new PlaceOrderRequest(preview.cartRevision(), preview.total()));
        assertThat(placed.total()).isEqualTo(75_000);
        assertThat(carts.findById(1L).orElseThrow().getItems()).isEmpty();
        assertThat(orders.get(100001L, placed.id()).total()).isEqualTo(75_000);
    }

    @Test
    void cancellationPreservesTheChosenPolicyResult() {
        var preview = orders.preview(100001L);
        var placed = orders.place(100001L, new PlaceOrderRequest(preview.cartRevision(), preview.total()));
        var cancelled = orders.cancel(100001L, placed.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.total()).isEqualTo(75_000);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoDiscountConfiguration {
        @Bean
        @Primary
        DiscountPolicy noDiscountPolicy() {
            return new NoDiscountPolicy();
        }
    }
}
