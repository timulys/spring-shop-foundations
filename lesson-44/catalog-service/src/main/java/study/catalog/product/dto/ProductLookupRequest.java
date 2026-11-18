package study.catalog.product.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import study.catalog.common.ApiException;
import tools.jackson.databind.JsonNode;

public record ProductLookupRequest(List<String> productCodes) {
    public ProductLookupRequest {
        if (productCodes == null || productCodes.isEmpty() || productCodes.size() > 100) {
            throw invalidCodes();
        }
        var normalized = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (String value : productCodes) {
            if (value == null || value.isBlank()) {
                throw invalidCodes();
            }
            String code = value.strip();
            if (code.length() > 20 || !seen.add(code)) {
                throw invalidCodes();
            }
            normalized.add(code);
        }
        productCodes = List.copyOf(normalized);
    }

    public static ProductLookupRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalidType();
        }
        JsonNode values = body.get("productCodes");
        if (values == null || values.isNull()) {
            throw invalidCodes();
        }
        if (!values.isArray()) {
            throw invalidType();
        }
        var codes = new ArrayList<String>();
        for (JsonNode value : values) {
            if (value.isNull()) {
                codes.add(null);
            } else if (value.isTextual()) {
                codes.add(value.asText());
            } else {
                throw invalidType();
            }
        }
        return new ProductLookupRequest(codes);
    }

    private static ApiException invalidCodes() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_CODES",
                "중복 없이 상품 코드를 1~100개 입력해 주세요. 각 코드는 1~20자여야 합니다.");
    }

    private static ApiException invalidType() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "productCodes에는 문자열 배열을 입력해 주세요.");
    }
}
