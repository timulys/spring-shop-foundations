package study.shop.order.infrastructure;

public record CatalogCallPolicy(long connectTimeoutMillis, long attemptTimeoutMillis,
                                long totalTimeoutMillis, long retryDelayMillis, int maxAttempts) {
    public CatalogCallPolicy {
        if (connectTimeoutMillis <= 0 || attemptTimeoutMillis <= 0 || totalTimeoutMillis <= 0
                || retryDelayMillis < 0 || maxAttempts < 1 || maxAttempts > 3) {
            throw new IllegalArgumentException("상품 호출 시간은 양수, 재시도 대기는 0 이상, 시도 횟수는 1~3이어야 합니다.");
        }
        Math.multiplyExact(totalTimeoutMillis, 1_000_000L);
        Math.multiplyExact(attemptTimeoutMillis, 1_000_000L);
        Math.multiplyExact(retryDelayMillis, 1_000_000L);
    }
}
