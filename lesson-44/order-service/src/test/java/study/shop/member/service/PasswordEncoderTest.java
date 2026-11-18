package study.shop.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

class PasswordEncoderTest {
    private final PasswordEncoder encoder = new PasswordConfiguration().passwordEncoder();
    @Test void randomSaltChangesHashButBothMatchOriginal() {
        String raw = "lesson31-example";
        String first = encoder.encode(raw);
        String second = encoder.encode(raw);
        assertThat(first).isNotEqualTo(raw).isNotEqualTo(second).startsWith("$2a$12$");
        assertThat(encoder.matches(raw, first)).isTrue();
        assertThat(encoder.matches(raw, second)).isTrue();
        assertThat(encoder.matches("different-input", first)).isFalse();
        System.out.println("같은 입력: 해시 문자열은 다름 / 원래 입력 대조: 둘 다 true / 다른 입력: false");
    }
    @Test void leadingAndTrailingSpacesRemainPartOfThePassword() {
        String hash = encoder.encode(" pass-word ");
        assertThat(encoder.matches(" pass-word ", hash)).isTrue();
        assertThat(encoder.matches("pass-word", hash)).isFalse();
    }
    @Test void bcryptAccepts72Utf8BytesAndRejects75Bytes() {
        String raw = "가".repeat(24);
        assertThat(encoder.matches(raw, encoder.encode(raw))).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode("가".repeat(25)));
    }
}
