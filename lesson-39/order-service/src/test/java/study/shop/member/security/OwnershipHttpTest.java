package study.shop.member.security;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import study.shop.common.CsrfTestClient;
import study.shop.common.OwnerTestData;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OwnershipHttpTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient alice = CsrfTestClient.client();
    private final HttpClient bob = CsrfTestClient.client();
    private final HttpClient anonymous = CsrfTestClient.client();
    private long aliceId, bobId;

    @BeforeEach void prepare() throws Exception {
        OwnerTestData.clear(jdbc);
        aliceId = signupAndLogin(alice,"alice_01");
        bobId = signupAndLogin(bob,"bob_02");
    }
    @AfterEach void cleanup() {
        OwnerTestData.clear(jdbc);
        alice.close(); bob.close(); anonymous.close();
    }
    @Test void publicCatalogAndProtectedPersonalResources() throws Exception {
        assertEquals(200,send(anonymous,"GET","/api/products",null).statusCode());
        for (String path : new String[]{"/api/cart","/api/orders","/api/orders/preview","/api/orders/123"}) {
            var reply=send(anonymous,"GET",path,null);
            assertError(reply,401,"AUTHENTICATION_REQUIRED");
        }
        assertError(send(anonymous,"POST","/api/cart/items",item("P001",1)),401,"AUTHENTICATION_REQUIRED");
    }
    @Test void cartsUseSessionOwnerEvenWhenRequestSuppliesAnotherMemberId() throws Exception {
        var reply=send(alice,"POST","/api/cart/items?memberId="+bobId,
                "{\"productCode\":\"P001\",\"quantity\":2,\"memberId\":"+bobId+"}");
        assertEquals(200,reply.statusCode());
        assertEquals(2,body(send(alice,"GET","/api/cart",null)).get("totalQuantity").asInt());
        assertEquals(0,body(send(bob,"GET","/api/cart?memberId="+aliceId,null)).get("totalQuantity").asInt());
        assertEquals(200,send(bob,"POST","/api/cart/items",item("P002",1)).statusCode());
        assertEquals("P001",body(send(alice,"GET","/api/cart",null)).get("items").get(0).get("productCode").asText());
        assertEquals(2,jdbc.queryForObject("select quantity from cart_items i join carts c on c.id=i.cart_id where c.member_id=?",Integer.class,aliceId));
        assertEquals(2,jdbc.queryForObject("select count(*) from carts",Integer.class));
    }
    @Test void foreignOrderAndMissingOrderHaveSameNotFoundContract() throws Exception {
        long id=place(alice,"P001",2);
        assertEquals(200,send(alice,"GET","/api/orders/"+id,null).statusCode());
        for (long target : new long[]{id,Long.MAX_VALUE}) {
            assertError(send(bob,"GET","/api/orders/"+target,null),404,"ORDER_NOT_FOUND");
            assertError(send(bob,"POST","/api/orders/"+target+"/cancel",null),404,"ORDER_NOT_FOUND");
        }
        assertEquals("CONFIRMED",jdbc.queryForObject("select status from orders where id=?",String.class,id));
        assertEquals(200,send(alice,"POST","/api/orders/"+id+"/cancel",null).statusCode());
        assertEquals("CANCELLED",jdbc.queryForObject("select status from orders where id=?",String.class,id));
        assertError(send(alice,"POST","/api/orders/"+id+"/cancel",null),409,"ORDER_STATE_CONFLICT");
    }
    @Test void orderListAndCountAreScopedBeforePagination() throws Exception {
        long a1=place(alice,"P001",1);
        long b=place(bob,"P002",1);
        long a2=place(alice,"P003",1);
        var first=body(send(alice,"GET","/api/orders?size=1",null));
        var second=body(send(alice,"GET","/api/orders?size=1&page=1",null));
        assertEquals(2,first.get("totalElements").asInt());
        assertEquals(a2,first.get("content").get(0).get("id").asLong());
        assertEquals(a1,second.get("content").get(0).get("id").asLong());
        var bobList=body(send(bob,"GET","/api/orders?memberId="+aliceId,null));
        assertEquals(1,bobList.get("totalElements").asInt());
        assertEquals(b,bobList.get("content").get(0).get("id").asLong());
        assertEquals(aliceId,jdbc.queryForObject("select member_id from orders where id=?",Long.class,a2));
    }
    @Test void missingCsrfCannotChangeCartOrCancelOrder() throws Exception {
        long id=place(alice,"P001",2);
        for (String path : new String[]{"/api/cart/items","/api/orders/"+id+"/cancel"}) {
            var reply=alice.send(request("POST",path,item("P002",1)),HttpResponse.BodyHandlers.ofString());
            assertError(reply,403,"CSRF_INVALID");
        }
        assertEquals(0,jdbc.queryForObject("select count(*) from cart_items",Integer.class));
        assertEquals("CONFIRMED",jdbc.queryForObject("select status from orders where id=?",String.class,id));
    }
    @Test void logoutRemovesPersonalAccessAndAnotherSessionIsIndependent() throws Exception {
        send(alice,"POST","/api/cart/items",item("P001",1));
        assertEquals(204,send(alice,"POST","/logout",null).statusCode());
        assertError(send(alice,"GET","/api/cart",null),401,"AUTHENTICATION_REQUIRED");
        assertEquals(200,send(bob,"GET","/api/cart",null).statusCode());
        assertEquals(1,jdbc.queryForObject("select count(*) from cart_items",Integer.class));
    }
    private long signupAndLogin(HttpClient client,String loginId) throws Exception {
        var signup=send(client,"POST","/api/members",json.writeValueAsString(Map.of(
                "loginId",loginId,"displayName",loginId,"password","test-pass-123")));
        assertEquals(201,signup.statusCode());
        var login=CsrfTestClient.send(client,HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/login"))
                .header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+loginId+"&password=test-pass-123")).build());
        assertEquals(204,login.statusCode());
        return body(signup).get("id").asLong();
    }
    private long place(HttpClient client,String product,int quantity) throws Exception {
        assertEquals(200,send(client,"POST","/api/cart/items",item(product,quantity)).statusCode());
        var preview=body(send(client,"GET","/api/orders/preview",null));
        var reply=send(client,"POST","/api/orders",json.writeValueAsString(Map.of(
                "cartRevision",preview.get("cartRevision").asLong(),"expectedTotalWon",preview.get("total").asLong(),
                "memberId",client==alice?bobId:aliceId)));
        assertEquals(201,reply.statusCode());
        return body(reply).get("id").asLong();
    }
    private String item(String code,int quantity) {
        return json.writeValueAsString(Map.of("productCode",code,"quantity",quantity));
    }
    private HttpRequest request(String method,String path,String value) {
        return HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
                .header("Content-Type","application/json")
                .method(method,value==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(value)).build();
    }
    private HttpResponse<String> send(HttpClient client,String method,String path,String value) throws Exception {
        return CsrfTestClient.send(client,request(method,path,value));
    }
    private JsonNode body(HttpResponse<String> reply) { return json.readTree(reply.body()); }
    private void assertError(HttpResponse<String> reply,int status,String code) {
        assertEquals(status,reply.statusCode(),reply.body());
        assertEquals(code,body(reply).get("code").asText());
        assertFalse(body(reply).get("requestId").asText().isBlank());
    }
}
