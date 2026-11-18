package study.shop.member.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import study.shop.member.dto.MemberResponse;
import study.shop.member.dto.SignupRequest;
import study.shop.member.service.SignupService;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final SignupService signupService;

    public MemberController(SignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return signupService.signup(request);
    }
}
