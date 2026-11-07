package study.shop;

import java.util.List;
import org.springframework.data.domain.Page;

public record OrderPageResponse(List<OrderSummaryResponse> content,
                                int number, int size, long totalElements,
                                int totalPages, boolean hasNext) {
    public OrderPageResponse {
        content = List.copyOf(content);
    }

    public static OrderPageResponse from(Page<OrderSummaryResponse> page) {
        return new OrderPageResponse(page.getContent(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.hasNext());
    }
}
