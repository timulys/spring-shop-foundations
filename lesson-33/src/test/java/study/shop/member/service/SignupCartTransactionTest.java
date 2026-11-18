package study.shop.member.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import study.shop.cart.domain.Cart;
import study.shop.cart.repository.CartRepository;
import study.shop.common.OwnerTestData;
import study.shop.member.dto.SignupRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignupCartTransactionTest {
    @Autowired SignupService signup;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean CartRepository carts;
    @BeforeEach void prepare() { OwnerTestData.clear(jdbc); }
    @AfterEach void cleanup() { OwnerTestData.clear(jdbc); }
    @Test void cartFailureRollsBackTheAlreadyInsertedMember() {
        doThrow(new IllegalStateException("intentional cart failure")).when(carts).saveAndFlush(any(Cart.class));
        assertThrows(IllegalStateException.class, () -> signup.signup(
                new SignupRequest("rollback_user","가입 롤백","sample-pass")));
        assertEquals(0,jdbc.queryForObject("select count(*) from members",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from carts",Integer.class));
    }
}
