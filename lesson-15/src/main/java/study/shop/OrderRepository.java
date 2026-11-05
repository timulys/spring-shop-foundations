package study.shop;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    // 같은 주문의 상태 변경을 차례로 처리합니다. 트랜잭션 안에서 호출합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id")
    Optional<ShopOrder> findForUpdate(@Param("id") long id);
}
