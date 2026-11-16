package study.shop.member.web;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import study.shop.member.domain.Member;
import static org.assertj.core.api.Assertions.*;
import static study.shop.member.MemberFixtures.HASH;

class SignupRequestTest {
    @Test
    void stripsIdentityFieldsButPreservesExactPassword() {
        SignupRequest request = new SignupRequest(" Tim_01 ", " 팀 ", " password ");
        assertThat(request.loginId()).isEqualTo("Tim_01");
        assertThat(request.normalizedLoginId()).isEqualTo("tim_01");
        assertThat(request.displayName()).isEqualTo("팀");
        assertThat(request.password()).isEqualTo(" password ");
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }

    @Test
    void validatesMissingValuesAndLengthBoundaries() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new SignupRequest(null, null, null))).hasSize(3);
            assertThat(validator.validate(new SignupRequest("ab", "팀", "1234567"))).hasSize(2);
            assertThat(validator.validate(new SignupRequest("tim", "가".repeat(51), "a".repeat(65)))).hasSize(2);
            assertThat(validator.validate(new SignupRequest("a".repeat(30), "가".repeat(50), "a".repeat(64)))).isEmpty();
        }
    }

    @Test
    void passwordIsInputOnlyAndRedactedInText() {
        var mapper = JsonMapper.builder().build();
        var request = mapper.readValue("{\"loginId\":\"tim\",\"displayName\":\"팀\",\"password\":\"sample-password\"}", SignupRequest.class);
        assertThat(request.password()).isEqualTo("sample-password");
        assertThat(mapper.writeValueAsString(request)).doesNotContain("password", "sample-password");
        assertThat(request.toString()).contains("[REDACTED]").doesNotContain("sample-password");
    }

    @Test
    void responseAndEntitySerializationExcludeHash() {
        var mapper = JsonMapper.builder().build();
        Member member = Member.withPasswordHash("tim", "팀", HASH);
        String response = mapper.writeValueAsString(MemberResponse.from(member));
        assertThat(response).contains("\"loginId\":\"tim\"", "\"displayName\":\"팀\"");
        assertThat(response).doesNotContain("password", HASH);
        assertThat(mapper.writeValueAsString(member)).doesNotContain("password", HASH);
    }
}
