package study.shop.cart.repository;

import study.shop.cart.domain.Cart;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByMemberId(long memberId);

    @Query("select distinct c from Cart c left join fetch c.items where c.memberId = :memberId")
    Optional<Cart> findWithItems(@Param("memberId") long memberId);

}
