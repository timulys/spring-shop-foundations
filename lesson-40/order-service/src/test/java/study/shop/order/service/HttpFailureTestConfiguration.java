package study.shop.order.service;

import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import study.shop.cart.repository.CartRepository;
import study.shop.order.domain.DiscountPolicy;

@TestConfiguration(proxyBeanMethods = false)
@Import(HttpFailureTestConfiguration.FailureEndpoint.class)
public class HttpFailureTestConfiguration {
    @TestComponent
    @RestController
    public static class FailureEndpoint {
        private final OrderWriter writer;
        private final CartRepository carts;
        private final DiscountPolicy policy;
        private final EntityManager em;

        FailureEndpoint(OrderWriter writer, CartRepository carts, DiscountPolicy policy, EntityManager em) {
            this.writer = writer; this.carts = carts; this.policy = policy; this.em = em;
        }

        @Transactional
        @PostMapping("/test/orders/fail-after-flush")
        public void failAfterFlush() {
            var cart = carts.findByMemberId(100001L).orElseThrow();
            writer.save(100001L, cart.getRevision(), OrderWriteFixtures.prepared(cart, policy));
            em.flush();
            throw new IllegalStateException("통합 테스트용 로컬 쓰기 실패");
        }
    }
}
