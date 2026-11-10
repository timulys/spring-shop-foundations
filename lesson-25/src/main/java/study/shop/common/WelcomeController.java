package study.shop.common;

import study.shop.catalog.Product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
    @GetMapping(value = "/hello", produces = "text/plain;charset=UTF-8")
    public String hello() {
        Product keyboard = new Product("P001", "키보드", 30_000L, "주변기기");

        return "안녕하세요. 쇼핑몰 첫 서버입니다.\n"
                + "상품: " + keyboard.getName() + "\n"
                + "가격: " + keyboard.getUnitPrice() + "원";
    }
}
