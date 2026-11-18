package study.shop.order.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class CatalogHttpConfiguration {
    @Bean(destroyMethod = "close")
    HttpClient catalogJdkClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Bean
    RestClient catalogRestClient(HttpClient catalogJdkClient,
                                @Value("${catalog.base-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(catalogJdkClient);
        factory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
