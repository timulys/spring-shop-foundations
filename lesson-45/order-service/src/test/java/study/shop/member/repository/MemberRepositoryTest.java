package study.shop.member.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import study.shop.member.domain.Member;
import static org.assertj.core.api.Assertions.*;
import static study.shop.member.MemberFixtures.HASH;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class MemberRepositoryTest extends study.shop.common.CatalogServerTestBase {
    @Autowired MemberRepository members;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void differentLoginIdsReceiveDifferentDatabaseIds() {
        Member first = members.saveAndFlush(Member.withPasswordHash("Tim_01", "팀", HASH));
        Member second = members.saveAndFlush(Member.withPasswordHash("hana_02", "하나", HASH));
        Long firstId = first.getId();
        Long secondId = second.getId();
        assertThat(firstId).isPositive().isNotEqualTo(secondId);
        entityManager.clear();
        Member loaded = members.findByLoginId("tim_01").orElseThrow();
        assertThat(loaded.getId()).isEqualTo(firstId);
        assertThat(loaded.getPasswordHash()).isEqualTo(HASH);
        assertThat(members.findById(secondId).orElseThrow().getLoginId()).isEqualTo("hana_02");
        assertThat(members.findByLoginId("missing")).isEmpty();
        System.out.printf("두 회원: id=%d/loginId=tim_01, id=%d/loginId=hana_02%n", firstId, secondId);
    }

    @Test
    void normalizedDuplicateIsRejectedByDatabase() {
        members.saveAndFlush(Member.withPasswordHash(" Tim_01 ", "첫 회원", HASH));
        assertThat(members.existsByLoginId("tim_01")).isTrue();
        assertThatThrownBy(() -> members.saveAndFlush(Member.withPasswordHash("TIM_01", "둘째 회원", HASH)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_members_login_id");
        // 실패한 트랜잭션에서는 추가 조회·저장을 하지 않고 테스트 종료 시 롤백합니다.
    }

    @Test
    void databaseAlsoRejectsUnnormalizedDirectInsert() {
        assertThatThrownBy(() -> jdbc.update("insert into members(login_id, display_name, password_hash) values (?, ?, ?)", "UPPER", "회원", HASH))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_members_login_id");
    }

    @Test
    void databaseRejectsPlaintextEvenWhenEntityIsBypassed() {
        assertThatThrownBy(() -> jdbc.update("insert into members(login_id, display_name, password_hash) values (?, ?, ?)", "plain_user", "회원", "plain-password"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_members_password_hash");
    }

    @Test
    void uniqueLoginIdAlsoRequiresNonNullValue() {
        assertThatThrownBy(() -> jdbc.update("insert into members(login_id, display_name, password_hash) values (?, ?, ?)", null, "회원", HASH))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
