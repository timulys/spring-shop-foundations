package study.shop.order.service;

import study.shop.order.domain.DiscountPolicy;
import study.shop.order.domain.ThresholdFixedDiscountPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrderConfiguration {
    @Bean
    DiscountPolicy discountPolicy() {
        return new ThresholdFixedDiscountPolicy();
    }
}
