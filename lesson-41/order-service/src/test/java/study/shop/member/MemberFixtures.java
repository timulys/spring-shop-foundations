package study.shop.member;

/** 회원 제약 테스트 전용입니다. 실제 가입 해시는 31강에서 입력별로 생성합니다. */
public final class MemberFixtures {
    private MemberFixtures() { }
    // Spring Security 공식 Password Storage 문서의 공개 예제 값입니다.
    public static final String HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
}
