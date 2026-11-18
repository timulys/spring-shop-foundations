package study.shop.common;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleBusiness(ApiException ex, WebRequest request) {
        return response(ex.status(), ex.code(), ex.getMessage(), List.of(),
                HttpHeaders.EMPTY, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldError::field)
                        .thenComparing(ApiError.FieldError::message))
                .toList();
        return response(status, "VALIDATION_ERROR", "입력값을 확인해 주세요.",
                fields, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return response(status, "INVALID_JSON", "JSON 형식과 필드 자료형을 확인해 주세요.",
                List.of(), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = switch (status.value()) {
            case 400 -> "요청 형식이나 값을 확인해 주세요.";
            case 404 -> "요청한 대상을 찾을 수 없습니다.";
            case 405 -> "허용하지 않는 요청 메서드입니다.";
            case 415 -> "지원하지 않는 콘텐츠 형식입니다.";
            default -> "요청을 처리할 수 없습니다.";
        };
        if (ex instanceof ResponseStatusException r && r.getReason() != null) {
            message = r.getReason();
        }
        return response(status, "HTTP_" + status.value(), message,
                List.of(), headers, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("requestId={} 요청 처리 실패", requestId(request), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "서버에서 요청을 처리하지 못했습니다.", List.of(), HttpHeaders.EMPTY, request);
    }

    private ResponseEntity<Object> response(HttpStatusCode status, String code,
            String message, List<ApiError.FieldError> fields,
            HttpHeaders headers, WebRequest request) {
        return new ResponseEntity<>(new ApiError(code, message, requestId(request), fields),
                headers, status);
    }

    private String requestId(WebRequest request) {
        return (String) request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST);
    }
}
