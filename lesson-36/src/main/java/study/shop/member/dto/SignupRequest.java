package study.shop.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import study.shop.member.domain.LoginIds;

public record SignupRequest(
        @NotBlank(message = "로그인 아이디가 필요합니다.")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{2,29}", message = "아이디는 영문자로 시작하는 3~30자의 영문·숫자·밑줄입니다.")
        String loginId,
        @NotBlank(message = "표시 이름이 필요합니다.")
        @Size(max = 50, message = "표시 이름은 50자 이하입니다.")
        String displayName,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "비밀번호가 필요합니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자입니다.")
        String password) {
    public SignupRequest {
        loginId = loginId == null ? null : loginId.strip();
        displayName = displayName == null ? null : displayName.strip();
        // 비밀번호의 공백은 사용자가 입력한 값의 일부이므로 그대로 둡니다.
    }

    public String normalizedLoginId() { return LoginIds.normalize(loginId); }

    @Override
    public String toString() {
        return "SignupRequest[loginId=" + loginId + ", password=[REDACTED]]";
    }
}
