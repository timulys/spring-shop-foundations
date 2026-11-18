package study.shop.member.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import study.shop.member.domain.LoginIds;
import study.shop.member.repository.MemberRepository;

@Service
public class MemberDetailsService implements UserDetailsService {
    private final MemberRepository members;
    public MemberDetailsService(MemberRepository members) { this.members = members; }

    @Override public UserDetails loadUserByUsername(String input) {
        String loginId;
        try { loginId = LoginIds.normalize(input); }
        catch (IllegalArgumentException ex) { throw missing(); }
        return members.findByLoginId(loginId).map(ShopPrincipal::new).orElseThrow(this::missing);
    }
    private UsernameNotFoundException missing() {
        return new UsernameNotFoundException("아이디와 비밀번호를 확인해 주세요.");
    }
}
