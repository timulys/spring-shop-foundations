package study.shop.member.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import study.shop.member.repository.MemberRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class SignupHttpTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean MemberRepository members;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void prepare() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("shop_test");
        jdbc.update("delete from members");
    }
    @AfterEach void cleanup() {
        jdbc.update("delete from members");
        client.close();
    }

    @Test void storesOnlyHashAndReturnsOnlyPublicFields(CapturedOutput output) throws Exception {
        String raw = " lesson31-input ";
        var response = signup("  Tim_01  ", raw);
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = json.readTree(response.body());
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("loginId").asText()).isEqualTo("tim_01");
        assertThat(body.get("displayName").asText()).isEqualTo("팀");
        assertThat(response.body()).doesNotContain("password", raw, "$2");
        String hash = jdbc.queryForObject("select password_hash from members where login_id='tim_01'", String.class);
        assertThat(hash).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hash)).isTrue();
        assertThat(encoder.matches(raw.strip(), hash)).isFalse();
        assertThat(output.getAll()).doesNotContain(raw);
        System.out.println("가입 HTTP 201 / 응답 필드 3개 / DB에는 해시 / 원래 입력 true / 공백 제거 입력 false");
    }

    @Test void duplicateNormalizedLoginReturns409AndKeepsFirstMember() throws Exception {
        assertThat(signup("Tim_01", "first-password").statusCode()).isEqualTo(201);
        var response = signup("TIM_01", "second-password");
        assertError(response,409,"LOGIN_ID_TAKEN");
        assertThat(count()).isEqualTo(1);
        String hash = jdbc.queryForObject("select password_hash from members",String.class);
        assertThat(encoder.matches("first-password",hash)).isTrue();
        assertThat(encoder.matches("second-password",hash)).isFalse();
    }

    @Test void byteBoundaryAllows72ButRejects75BeforeInsert() throws Exception {
        assertThat(signup("utf8_ok", "가".repeat(24)).statusCode()).isEqualTo(201);
        assertError(signup("utf8_bad", "가".repeat(25)),400,"PASSWORD_INPUT_INVALID");
        assertThat(count()).isEqualTo(1);
    }

    @Test void beanValidationRejectsBlankShortAndLongInputWithoutEcho(CapturedOutput output) throws Exception {
        for (String raw : List.of(" ".repeat(8),"abcdefg","a".repeat(65))) {
            var response = signup("bad_input",raw);
            assertError(response,400,"VALIDATION_ERROR");
            if (!raw.isBlank()) {
                assertThat(response.body()).doesNotContain(raw);
                assertThat(output.getAll()).doesNotContain(raw);
            }
        }
        assertThat(count()).isZero();
    }

    @Test void invalidAndMissingIdentityDoNotCreateMembers() throws Exception {
        assertError(signup("1invalid","valid-password"),400,"VALIDATION_ERROR");
        var empty = request("POST","/api/members","{}");
        assertError(empty,400,"VALIDATION_ERROR");
        assertThat(count()).isZero();
    }

    @Test void twoRequestsPassingPrecheckStillCreateOnlyOneMember() throws Exception {
        CountDownLatch checked = new CountDownLatch(2);
        doAnswer(invocation -> {
            // 실제 DB 조회 직후 두 요청을 기다리게 해 같은 경쟁 조건을 재현합니다.
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                    "select exists(select 1 from members where login_id=?)", Boolean.class,
                    invocation.getArgument(0, String.class)));
            checked.countDown();
            if (!checked.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("precheck barrier timeout");
            return exists;
        }).when(members).existsByLoginId("race_user");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> signup("race_user","first-race-password"));
            var second = pool.submit(() -> signup("RACE_USER","second-race-password"));
            var a=first.get(20,TimeUnit.SECONDS);var b=second.get(20,TimeUnit.SECONDS);
            assertThat(List.of(a.statusCode(),b.statusCode())).containsExactlyInAnyOrder(201,409);
            assertError(a.statusCode()==409?a:b,409,"LOGIN_ID_TAKEN");
            assertThat(count()).isEqualTo(1);
            assertThat(checked.getCount()).isZero();
            System.out.println("동시 가입: 사전 조회 둘 다 통과 / HTTP 201·409 / 저장 회원 1명");
        }
    }

    @Test void cryptoDependencyDoesNotEnableWebAuthenticationYet() throws Exception {
        var products=request("GET","/api/products",null);
        assertThat(products.statusCode()).isEqualTo(200);
        var page=request("GET","/signup.html",null);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("autocomplete=\"new-password\"","/signup.js");
    }

    private int count() { return jdbc.queryForObject("select count(*) from members",Integer.class); }
    private HttpResponse<String> signup(String loginId,String password) throws Exception {
        return request("POST","/api/members",json.writeValueAsString(Map.of("loginId",loginId,"displayName"," 팀 ","password",password)));
    }
    private HttpResponse<String> request(String method,String path,String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
                .timeout(Duration.ofSeconds(25)).header("Content-Type","application/json")
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body))
                .build(),HttpResponse.BodyHandlers.ofString());
    }
    private void assertError(HttpResponse<String> response,int expected,String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(expected);
        var body=json.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("requestId").asText()).isNotBlank();
    }
}
