package study.shop.member.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityResponses responses) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/session/**").authenticated()
                // 33강에서 장바구니와 주문을 인증 회원의 범위로 바꿉니다.
                .anyRequest().permitAll());
        http.csrf(Customizer.withDefaults());
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.loginPage("/login.html").loginProcessingUrl("/login")
                .successHandler((request,response,authentication) -> response.setStatus(204))
                .failureHandler((request,response,error) -> responses.error(request,response,401,
                        "LOGIN_FAILED","아이디와 비밀번호를 확인해 주세요."))
                .permitAll());
        http.logout(logout -> logout.logoutUrl("/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request,response,authentication) -> response.setStatus(204)));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,error) -> responses.error(request,response,401,
                        "AUTHENTICATION_REQUIRED","로그인이 필요합니다."))
                .accessDeniedHandler((request,response,error) -> responses.error(request,response,403,
                        error instanceof CsrfException ? "CSRF_INVALID" : "ACCESS_DENIED",
                        "요청을 허용할 수 없습니다. 화면을 새로고침해 주세요.")));
        return http.build();
    }
}
