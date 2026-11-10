package study.shop.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public record PlaceOrderRequest(
        @NotNull(message = "확인한 장바구니 버전이 필요합니다.")
        @Min(value = 0, message = "장바구니 버전은 0 이상이어야 합니다.")
        @JsonDeserialize(using = StrictLongDeserializer.class)
        Long cartRevision,
        @NotNull(message = "확인한 주문 금액이 필요합니다.")
        @Min(value = 0, message = "주문 금액은 0 이상이어야 합니다.")
        @JsonDeserialize(using = StrictLongDeserializer.class)
        Long expectedTotalWon) {
}
