package study.shop.member.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.shop.member.dto.MemberResponse;
import study.shop.member.security.ShopPrincipal;

@RestController
public class SessionController {
    @GetMapping("/api/session/me")
    public MemberResponse me(@AuthenticationPrincipal ShopPrincipal principal) {
        return new MemberResponse(principal.memberId(), principal.getUsername(), principal.displayName());
    }

    @GetMapping("/api/csrf")
    public ResponseEntity<TokenResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new TokenResponse(token.getHeaderName(), token.getToken()));
    }
    public record TokenResponse(String headerName, String token) { }
}
