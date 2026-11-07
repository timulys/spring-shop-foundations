package study.shop;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    @Query(value = """
            select new study.shop.OrderSummaryResponse(
                o.id, o.status, o.total, o.createdAt)
            from ShopOrder o
            """, countQuery = "select count(o) from ShopOrder o")
    Page<OrderSummaryResponse> findSummaries(Pageable pageable);

    @Query("select o from ShopOrder o left join fetch o.items where o.id = :id")
    Optional<ShopOrder> findDetailById(@Param("id") long id);

    // 같은 주문의 상태 변경을 차례로 처리합니다. 트랜잭션 안에서 호출합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id")
    Optional<ShopOrder> findForUpdate(@Param("id") long id);
}
