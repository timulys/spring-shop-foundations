package study.shop.member.service;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import study.shop.common.ApiException;

public final class PasswordInputPolicy {
    private PasswordInputPolicy() { }

    public static void validate(String password) {
        if (password == null || password.isBlank()
                || password.length() < 8 || password.length() > 64) {
            throw invalid("비밀번호는 공백만으로 만들 수 없으며 길이는 8~64자입니다.");
        }
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(password)) {
            throw invalid("비밀번호에 올바른 유니코드 문자를 입력해 주세요.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_INPUT_INVALID", message);
    }
}
