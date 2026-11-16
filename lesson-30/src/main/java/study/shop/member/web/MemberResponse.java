package study.shop.member.web;

import study.shop.member.domain.Member;

public record MemberResponse(Long id, String loginId, String displayName) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getLoginId(), member.getDisplayName());
    }
}
