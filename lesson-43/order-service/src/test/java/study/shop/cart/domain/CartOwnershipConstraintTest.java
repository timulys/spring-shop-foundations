package study.shop.cart.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Sql("/owner-fixture.sql")
@Transactional
class CartOwnershipConstraintTest extends study.shop.common.CatalogServerTestBase {
    @Autowired JdbcTemplate jdbc;
    @Test void databaseRejectsASecondCartForOneMember() {
        assertThatThrownBy(() -> jdbc.update("insert into carts(member_id,revision) values (100001,0)"))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uk_carts_member");
    }
    @Test void databaseRejectsCartForMissingMember() {
        assertThatThrownBy(() -> jdbc.update("insert into carts(member_id,revision) values (999999,0)"))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("fk_carts_member");
    }
}
