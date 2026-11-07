package study.shop;

import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// src/test에만 있는 설정입니다. bootJar와 일반 서버에는 포함되지 않습니다.
@TestConfiguration(proxyBeanMethods = false)
@Import(HttpFailureTestConfiguration.FailureEndpoint.class)
public class HttpFailureTestConfiguration {
    @TestComponent
    @RestController
    public static class FailureEndpoint {
        private final OrderService orders;
        private final EntityManager em;

        FailureEndpoint(OrderService orders, EntityManager em) {
            this.orders = orders;
            this.em = em;
        }

        @Transactional
        @PostMapping("/test/orders/fail-after-flush")
        public void failAfterFlush() {
            orders.place();
            em.flush();
            System.out.println("테스트 전용 HTTP 경로: 주문과 장바구니 변경 flush 후 의도한 실패");
            throw new IllegalStateException("통합 테스트용 실패");
        }
    }
}
