package study.shop.order.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CatalogHttpConfiguration {
    @Bean
    CatalogCallPolicy catalogCallPolicy(
            @Value("${catalog.connect-timeout-ms}") long connect,
            @Value("${catalog.attempt-timeout-ms}") long attempt,
            @Value("${catalog.total-timeout-ms}") long total,
            @Value("${catalog.retry-delay-ms}") long delay,
            @Value("${catalog.max-attempts}") int count) {
        return new CatalogCallPolicy(connect, attempt, total, delay, count);
    }

    @Bean(destroyMethod = "close")
    HttpClient catalogJdkClient(CatalogCallPolicy policy) {
        return HttpClient.newBuilder().connectTimeout(Duration.ofMillis(policy.connectTimeoutMillis()))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
}
