package study.shop.member.service;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import study.shop.common.ApiException;
import study.shop.member.domain.Member;
import study.shop.member.dto.MemberResponse;
import study.shop.member.dto.SignupRequest;
import study.shop.member.repository.MemberRepository;

@Service
public class SignupService {
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;

    public SignupService(MemberRepository members, PasswordEncoder encoder,
            PlatformTransactionManager manager) {
        this.members = members;
        this.encoder = encoder;
        this.transaction = new TransactionTemplate(manager);
    }

    public MemberResponse signup(SignupRequest request) {
        String loginId = request.normalizedLoginId();
        PasswordInputPolicy.validate(request.password());
        if (members.existsByLoginId(loginId)) throw duplicate();

        // 비용이 큰 해시 계산을 마친 뒤 DB 쓰기 트랜잭션을 시작합니다.
        String hash = encoder.encode(request.password());
        try {
            return transaction.execute(status -> {
                Member saved = members.saveAndFlush(Member.withPasswordHash(
                        loginId, request.displayName(), hash));
                return MemberResponse.from(saved);
            });
        } catch (DataIntegrityViolationException ex) {
            // 동시에 가입해 사전 조회를 통과해도 DB 유일 제약이 중복을 거부합니다.
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && "uk_members_login_id".equals(constraint.getConstraintName())) {
                    throw duplicate();
                }
            }
            throw ex;
        }
    }

    private ApiException duplicate() {
        return new ApiException(HttpStatus.CONFLICT, "LOGIN_ID_TAKEN",
                "이미 사용 중인 로그인 아이디입니다.");
    }
}
