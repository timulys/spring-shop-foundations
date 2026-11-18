package study.shop.member.service;

import org.junit.jupiter.api.Test;
import study.shop.common.ApiException;
import static org.assertj.core.api.Assertions.*;

class PasswordInputPolicyTest {
    @Test void acceptsUtf8BoundaryWithoutChangingSpaces() {
        assertThatNoException().isThrownBy(() -> PasswordInputPolicy.validate("가".repeat(24)));
        assertThatNoException().isThrownBy(() -> PasswordInputPolicy.validate("a".repeat(64)));
        assertThatNoException().isThrownBy(() -> PasswordInputPolicy.validate(" pass-word "));
    }
    @Test void rejectsByteOverflowEvenWhenCharacterLengthFits() {
        assertThatThrownBy(() -> PasswordInputPolicy.validate("가".repeat(25)))
                .isInstanceOf(ApiException.class).hasMessageContaining("72바이트");
    }
    @Test void rejectsEmptyShortLongAndInvalidUnicode() {
        for (String input : new String[]{null, " ".repeat(8), "abcdefg", "a".repeat(65), "abcdefg" + (char)0xd800}) {
            assertThatThrownBy(() -> PasswordInputPolicy.validate(input)).isInstanceOf(ApiException.class);
        }
    }
}
