package study.shop.order.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "order_requests", uniqueConstraints =
        @UniqueConstraint(name = "uk_order_requests_member_key", columnNames = {"member_id", "request_key"}))
public class OrderRequestRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "member_id", nullable = false, updatable = false) private long memberId;
    @Column(name = "request_key", nullable = false, updatable = false, length = 80) private String requestKey;
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;
    @Column(name = "order_id", nullable = false, updatable = false) private long orderId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected OrderRequestRecord() {}
    public OrderRequestRecord(long memberId, String key, String hash, long orderId) {
        this.memberId = memberId; this.requestKey = key; this.requestHash = hash;
        this.orderId = orderId; this.createdAt = Instant.now();
    }
    public String getRequestHash() { return requestHash; }
    public long getOrderId() { return orderId; }
}
