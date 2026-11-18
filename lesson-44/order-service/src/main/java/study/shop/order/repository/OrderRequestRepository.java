package study.shop.order.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import study.shop.order.domain.OrderRequestRecord;
public interface OrderRequestRepository extends JpaRepository<OrderRequestRecord, Long> {
    Optional<OrderRequestRecord> findByMemberIdAndRequestKey(long memberId, String requestKey);
}
