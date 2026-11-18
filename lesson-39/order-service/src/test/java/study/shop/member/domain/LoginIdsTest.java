package study.shop.member.domain;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LoginIdsTest {
    @Test
    void normalizesWhitespaceAndCaseToOneIdentity() {
        assertThat(LoginIds.normalize("  Tim_01  ")).isEqualTo("tim_01");
        assertThat(LoginIds.normalize("tim_01")).isEqualTo("tim_01");
        assertThat(LoginIds.normalize("A".repeat(30))).isEqualTo("a".repeat(30));
    }

    @Test
    void rejectsMissingAndInvalidIdentifiers() {
        assertThatIllegalArgumentException().isThrownBy(() -> LoginIds.normalize(null));
        for (String value : new String[]{"", "ab", "a".repeat(31), "1abc", "ab c", "홍길동", "abc-01"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> LoginIds.normalize(value));
        }
    }

    @Test
    void normalizationDoesNotDependOnMachineLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(LoginIds.normalize("TITLE")).isEqualTo("title");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
