package study.shop.member.security;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import study.shop.common.CsrfTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionHttpTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    private final HttpClient client=CsrfTestClient.client();
    private final String password="session-test-pass";

    @BeforeEach void prepare() throws Exception {
        assertThat(jdbc.queryForObject("select current_database()",String.class)).isEqualTo("shop_test");
        jdbc.update("delete from members");
        var response=send("POST","/api/members",json.writeValueAsString(Map.of(
                "loginId","Tim_01","displayName","팀","password",password)),true,"application/json");
        assertThat(response.statusCode()).isEqualTo(201);
    }
    @AfterEach void cleanup() { jdbc.update("delete from members");client.close(); }

    @Test void loginPersistsAcrossRequestsAndLogoutRemovesAuthentication() throws Exception {
        assertError(send("GET","/api/session/me",null,false,null),401,"AUTHENTICATION_REQUIRED");
        String before=sessionId();
        assertThat(login(" TIM_01 ",password,true).statusCode()).isEqualTo(204);
        assertThat(sessionId()).isNotBlank().isNotEqualTo(before);
        for(int i=0;i<2;i++) {
            var me=send("GET","/api/session/me",null,false,null);
            assertThat(me.statusCode()).isEqualTo(200);
            var body=json.readTree(me.body());
            assertThat(body.size()).isEqualTo(3);
            assertThat(body.get("loginId").asText()).isEqualTo("tim_01");
            assertThat(me.body()).doesNotContain("password",password,"$2");
        }
        assertThat(send("POST","/logout",null,true,null).statusCode()).isEqualTo(204);
        assertError(send("GET","/api/session/me",null,false,null),401,"AUTHENTICATION_REQUIRED");
        System.out.println("로그인 204 → 내 정보 200·200 → 로그아웃 204 → 내 정보 401");
    }

    @Test void wrongPasswordAndUnknownUserReturnSameFailure() throws Exception {
        for(String[] input:new String[][]{{"tim_01","wrong-password"},{"missing_user",password},{"1invalid",password}}) {
            var response=login(input[0],input[1],true);
            assertError(response,401,"LOGIN_FAILED");
            assertThat(response.body()).doesNotContain(input[1],"passwordHash");
        }
        assertError(send("GET","/api/session/me",null,false,null),401,"AUTHENTICATION_REQUIRED");
    }

    @Test void overlongPasswordIsAuthenticationFailureRatherThanServerError() throws Exception {
        assertError(login("tim_01","가".repeat(25),true),401,"LOGIN_FAILED");
        assertError(login("tim_01","x".repeat(1000),true),401,"LOGIN_FAILED");
    }

    @Test void missingCsrfTokenBlocksLogin() throws Exception {
        assertError(login("tim_01",password,false),403,"CSRF_INVALID");
        assertError(send("GET","/api/session/me",null,false,null),401,"AUTHENTICATION_REQUIRED");
    }

    @Test void missingCsrfTokenDoesNotLogOutAnAuthenticatedSession() throws Exception {
        assertThat(login("tim_01",password,true).statusCode()).isEqualTo(204);
        assertError(send("POST","/logout",null,false,null),403,"CSRF_INVALID");
        assertThat(send("GET","/api/session/me",null,false,null).statusCode()).isEqualTo(200);
    }

    @Test void loginRotatesCsrfTokenAndFreshTokenAllowsLogout() throws Exception {
        JsonNode old=json.readTree(send("GET","/api/csrf",null,false,null).body());
        assertThat(login("tim_01",password,true).statusCode()).isEqualTo(204);
        var stale=client.send(HttpRequest.newBuilder(uri("/logout"))
                .header(old.get("headerName").asText(),old.get("token").asText())
                .POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        assertError(stale,403,"CSRF_INVALID");
        assertThat(send("POST","/logout",null,true,null).statusCode()).isEqualTo(204);
    }

    @Test void anotherCookieStoreDoesNotInheritAuthentication() throws Exception {
        assertThat(login("tim_01",password,true).statusCode()).isEqualTo(204);
        try(var anonymous=CsrfTestClient.client()) {
            var response=anonymous.send(HttpRequest.newBuilder(uri("/api/session/me")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertError(response,401,"AUTHENTICATION_REQUIRED");
        }
    }

    @Test void sessionCookieIsHttpOnlyAndSameSiteLax() throws Exception {
        try(var anonymous=HttpClient.newHttpClient()) {
            var response=anonymous.send(HttpRequest.newBuilder(uri("/api/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
            String cookie=response.headers().firstValue("Set-Cookie").orElseThrow();
            assertThat(cookie).contains("JSESSIONID=","HttpOnly","SameSite=Lax");
            assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        }
    }

    @Test void protectedGetAndPublicCatalogHaveSeparateAccessRules() throws Exception {
        assertThat(send("GET","/api/products",null,false,null).statusCode()).isEqualTo(200);
        assertThat(send("GET","/login.html",null,false,null).statusCode()).isEqualTo(200);
        assertError(send("GET","/api/session/me",null,false,null),401,"AUTHENTICATION_REQUIRED");
    }

    private HttpResponse<String> login(String username,String raw,boolean csrf) throws Exception {
        return send("POST","/login","username="+URLEncoder.encode(username,StandardCharsets.UTF_8)
                +"&password="+URLEncoder.encode(raw,StandardCharsets.UTF_8),csrf,"application/x-www-form-urlencoded");
    }
    private HttpResponse<String> send(String method,String path,String body,boolean csrf,String contentType) throws Exception {
        var request=HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15))
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if(contentType!=null)request.header("Content-Type",contentType);
        return csrf?CsrfTestClient.send(client,request.build()):client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    private URI uri(String path){return URI.create("http://127.0.0.1:"+port+path);}
    private String sessionId(){return ((CookieManager)client.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
            .filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();}
    private void assertError(HttpResponse<String> response,int status,String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);var body=json.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("requestId").asText()).isNotBlank()
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
    }
}
