package study.shop.common.money;

/** 이 상점에서 사용하는 0원 이상의 원 단위 금액입니다. */
public record Money(long won) {
    public static final Money ZERO = new Money(0);

    public Money {
        if (won < 0) {
            throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");
        }
    }

    public static Money ofWon(long won) {
        return new Money(won);
    }

    public Money plus(Money other) {
        return ofWon(Math.addExact(won, other.won));
    }

    public Money minus(Money other) {
        return ofWon(Math.subtractExact(won, other.won));
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("곱할 수량은 0 이상이어야 합니다.");
        }
        return ofWon(Math.multiplyExact(won, quantity));
    }

    public boolean isAtLeast(Money other) {
        return won >= other.won;
    }
}
