package study.shop.member.security;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import study.shop.common.ApiError;
import study.shop.common.RequestIdFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SecurityResponses {
    private final JsonMapper json = JsonMapper.builder().build();
    public void error(HttpServletRequest request, HttpServletResponse response,
            int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), new ApiError(code, message,
                (String) request.getAttribute(RequestIdFilter.ATTRIBUTE), List.of()));
    }
}
