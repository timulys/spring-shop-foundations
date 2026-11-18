package study.shop.order.repository;

import study.shop.order.domain.ShopOrder;
import study.shop.order.dto.OrderSummaryResponse;

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
            select new study.shop.order.dto.OrderSummaryResponse(
                o.id, o.status, o.total, o.createdAt)
            from ShopOrder o where o.memberId = :memberId
            """, countQuery = "select count(o) from ShopOrder o where o.memberId = :memberId")
    Page<OrderSummaryResponse> findSummaries(@Param("memberId") long memberId, Pageable pageable);

    @Query("select o from ShopOrder o left join fetch o.items where o.memberId = :memberId and o.id = :id")
    Optional<ShopOrder> findDetailById(@Param("memberId") long memberId, @Param("id") long id);

    // 같은 주문의 상태 변경을 차례로 처리합니다. 트랜잭션 안에서 호출합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.memberId = :memberId and o.id = :id")
    Optional<ShopOrder> findForUpdate(@Param("memberId") long memberId, @Param("id") long id);
}
