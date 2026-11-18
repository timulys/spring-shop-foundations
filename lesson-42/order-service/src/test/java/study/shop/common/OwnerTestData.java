package study.shop.common;

import java.net.URI;
import java.net.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class OwnerTestData {
    private OwnerTestData() { }
    public static void clear(JdbcTemplate jdbc) {
        if (!"shop_test".equals(jdbc.queryForObject("select current_database()",String.class)))
            throw new IllegalStateException("shop_test DB가 필요합니다.");
        for (String table : new String[]{"order_items","orders","cart_items","carts","members"})
            jdbc.update("delete from " + table);
    }
    public static void loginFixture(HttpClient client, int port, JdbcTemplate jdbc) throws Exception {
        jdbc.update("update members set password_hash=? where id=100001",
                new BCryptPasswordEncoder(4).encode("fixture-pass"));
        var response = CsrfTestClient.send(client, HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/login"))
                .header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=fixture_a&password=fixture-pass")).build());
        if(response.statusCode()!=204) throw new IllegalStateException("Fixture login failed: "+response.statusCode());
    }
}
