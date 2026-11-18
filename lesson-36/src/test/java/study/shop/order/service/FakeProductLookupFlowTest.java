package study.shop.order.service;

import study.shop.cart.domain.Cart;
import study.shop.cart.repository.CartRepository;
import study.shop.common.ApiException;
import study.shop.common.money.Money;
import study.shop.order.domain.ProductSnapshot;
import study.shop.order.dto.PlaceOrderRequest;
import study.shop.order.repository.OrderRepository;
import study.shop.order.service.port.ProductLookup;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@org.springframework.test.context.jdbc.Sql("/owner-fixture.sql")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Import(FakeProductLookupFlowTest.FakeConfiguration.class)
class FakeProductLookupFlowTest {
    @Autowired OrderService orders;
    @Autowired CartRepository carts;
    @Autowired OrderRepository orderRepository;
    @Autowired FakeProductLookup lookup;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepareCartAndSnapshots() {
        Cart cart = carts.findForUpdate(100001L).orElseThrow();
        cart.clear();
        cart.add("P001", 2);
        cart.add("P002", 1);
        lookup.values.clear();
        lookup.values.put("P001", new ProductSnapshot("P001", "키보드", Money.ofWon(30_000)));
        lookup.values.put("P002", new ProductSnapshot("P002", "마우스", Money.ofWon(15_000)));
    }

    @Test
    void sameSnapshotsKeepPreviewAndStoredOrderAt70000() {
        var preview = orders.preview(100001L);
        assertThat(preview.total()).isEqualTo(70_000);
        var placed = orders.place(100001L, new PlaceOrderRequest(preview.cartRevision(), preview.total()));
        assertThat(placed.total()).isEqualTo(70_000);
        assertThat(orders.get(100001L, placed.id()).total()).isEqualTo(70_000);
        assertThat(carts.findById(1L).orElseThrow().getItems()).isEmpty();
    }

    @Test
    void injectedSnapshotIsUsedEvenWhenDatabasePriceDiffers() {
        jdbc.update("update products set unit_price_won = 990000 where code = 'P001'");
        var preview = orders.preview(100001L);
        assertThat(preview.total()).isEqualTo(70_000);
        var placed = orders.place(100001L, new PlaceOrderRequest(preview.cartRevision(), preview.total()));
        assertThat(placed.total()).isEqualTo(70_000);
        assertThat(placed.items()).anySatisfy(item -> {
            assertThat(item.productCode()).isEqualTo("P001");
            assertThat(item.unitPrice()).isEqualTo(30_000);
        });
    }

    @Test
    void missingLookupRejectsOrderAndPreservesCart() {
        var preview = orders.preview(100001L);
        long beforeOrders = orderRepository.count();
        lookup.values.remove("P002");
        assertThatThrownBy(() -> orders.place(100001L,
                new PlaceOrderRequest(preview.cartRevision(), preview.total())))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("PRODUCT_NOT_FOUND"));
        assertThat(orderRepository.count()).isEqualTo(beforeOrders);
        assertThat(carts.findById(1L).orElseThrow().getItems()).hasSize(2);
    }

    static final class FakeProductLookup implements ProductLookup {
        final Map<String, ProductSnapshot> values = new HashMap<>();

        @Override
        public Optional<ProductSnapshot> findByCode(String code) {
            return Optional.ofNullable(values.get(code));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeConfiguration {
        @Bean
        @Primary
        FakeProductLookup fakeProductLookup() {
            return new FakeProductLookup();
        }
    }
}
