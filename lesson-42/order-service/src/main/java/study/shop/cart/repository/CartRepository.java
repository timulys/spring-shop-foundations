package study.shop.cart.repository;

import study.shop.cart.domain.Cart;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByMemberId(long memberId);

    @Query("select distinct c from Cart c left join fetch c.items where c.memberId = :memberId")
    Optional<Cart> findWithItems(@Param("memberId") long memberId);

    // 준비 코드: 같은 장바구니의 변경 요청이 DB에서 순서대로 처리되게 합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.memberId = :memberId")
    Optional<Cart> findForUpdate(@Param("memberId") long memberId);
}
