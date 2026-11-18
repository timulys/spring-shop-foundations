-- 이전 취소 주문의 실제 취소 시각은 알 수 없으므로 NULL을 유지합니다.
ALTER TABLE orders ADD COLUMN cancelled_at timestamptz;
ALTER TABLE orders ADD CONSTRAINT ck_orders_cancelled_at
    CHECK (cancelled_at IS NULL OR status = 'CANCELLED');
