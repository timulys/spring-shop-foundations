package study.shop.member.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import study.shop.member.domain.Member;

public final class ShopPrincipal implements UserDetails, CredentialsContainer, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final Long memberId;
    private final String loginId;
    private final String displayName;
    private String passwordHash;

    public ShopPrincipal(Member member) {
        memberId = member.getId();
        loginId = member.getLoginId();
        displayName = member.getDisplayName();
        passwordHash = member.getPasswordHash();
    }
    public Long memberId() { return memberId; }
    public String displayName() { return displayName; }
    @Override public String getUsername() { return loginId; }
    @Override @JsonIgnore public String getPassword() { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }
    @Override public void eraseCredentials() { passwordHash = null; }
    @Override public String toString() { return "ShopPrincipal[memberId=" + memberId + ", loginId=" + loginId + "]"; }
}
