package study.shop.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import study.shop.common.ApiException;
import study.shop.order.dto.PlaceOrderRequest;

public record OrderIntent(String key, String hash) {
    public static OrderIntent from(String key, PlaceOrderRequest request) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,80}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "주문 요청 키는 영문·숫자·밑줄·하이픈 16~80자로 보내 주세요.");
        }
        String normalized = "order-v1\n" + request.cartRevision() + "\n" + request.expectedTotalWon();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return new OrderIntent(key, HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }
}
