package study.shop.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import study.shop.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String normalizedLoginId);
    boolean existsByLoginId(String normalizedLoginId);
}
