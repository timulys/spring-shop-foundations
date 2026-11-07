package study.shop;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public record AddCartItemRequest(
        @NotBlank(message = "상품 코드는 필수입니다.")
        @Size(max = 20, message = "상품 코드는 20자까지 입력할 수 있습니다.")
        String productCode,
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        @Max(value = 99, message = "수량은 99개 이하여야 합니다.")
        @JsonDeserialize(using = StrictQuantityDeserializer.class)
        Integer quantity) {
}
