package study.shop.member.domain;

import java.util.Locale;

public final class LoginIds {
    private LoginIds() { }

    public static String normalize(String input) {
        if (input == null) throw new IllegalArgumentException("로그인 아이디가 필요합니다.");
        String stripped = input.strip();
        if (!stripped.matches("[A-Za-z][A-Za-z0-9_]{2,29}")) {
            throw new IllegalArgumentException(
                    "아이디는 영문자로 시작하는 3~30자의 영문·숫자·밑줄입니다.");
        }
        return stripped.toLowerCase(Locale.ROOT);
    }
}
