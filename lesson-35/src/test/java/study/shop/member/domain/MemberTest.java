package study.shop.member.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static study.shop.member.MemberFixtures.HASH;

class MemberTest {
    @Test
    void preparesNormalizedIdentityAndHashedStorage() {
        Member member = Member.withPasswordHash(" Tim_01 ", "  팀  ", HASH);
        assertThat(member.getId()).isNull();
        assertThat(member.getLoginId()).isEqualTo("tim_01");
        assertThat(member.getDisplayName()).isEqualTo("팀");
        assertThat(member.getPasswordHash()).isEqualTo(HASH);
        assertThat(member.toString()).doesNotContain(HASH, "passwordHash");
    }

    @Test
    void rejectsPlaintextAndMissingHash() {
        assertThatIllegalArgumentException().isThrownBy(() -> Member.withPasswordHash("tim_01", "팀", "plain-password"));
        assertThatIllegalArgumentException().isThrownBy(() -> Member.withPasswordHash("tim_01", "팀", null));
    }

    @Test
    void requiresDisplayNameWithinBoundary() {
        for (String name : new String[]{null, "  ", "가".repeat(51)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> Member.withPasswordHash("tim_01", name, HASH));
        }
        assertThat(Member.withPasswordHash("tim_01", "가".repeat(50), HASH).getDisplayName()).hasSize(50);
    }
}
