# URL은 유지하고 요청을 받을 서비스를 바꾸기

화면 코드는 계속 `/api/products`, `/api/cart`, `/api/orders`를 호출합니다. NGINX가 URL을 보고 요청을 전달할 서비스를 고르므로, 브라우저는 상품 앱과 주문 앱의 내부 주소를 알 필요가 없습니다.

| 브라우저 요청 | 전달 위치 | 담당 기능 |
|---|---|---|
| `/api/products` | catalog | 목록·검색 |
| `/api/products/상품코드` | catalog | 상품 하나 조회 |
| `/api/members`, `/api/csrf`, `/api/session/me` | order | 가입·CSRF·로그인 상태 |
| `/api/cart`, `/api/orders`와 하위 경로 | order | 내 장바구니·주문 |
| `/login`, `/logout` | order | 세션 생성·종료 |
| `/internal`·`/actuator`와 하위 경로 | NGINX에서 404 | 공개 요청 차단 |
| HTML·CSS·JavaScript | NGINX 파일 | 같은 쇼핑 화면 |

## 목록 경로와 상품 하나의 경로

```nginx
location = /api/products {
    include /etc/nginx/snippets/proxy.conf;
    proxy_pass http://catalog_app;
}
location ^~ /api/products/ {
    include /etc/nginx/snippets/proxy.conf;
    proxy_pass http://catalog_app;
}
```

첫 설정은 `/api/products`와 정확히 일치하는 경로를 처리합니다. 두 번째는 `/api/products/`로 시작하는 상품 상세 경로를 처리합니다. `/api/products-extra`는 두 조건에 해당하지 않으므로 일반 `/api/` 규칙을 거쳐 주문 앱의 404를 받습니다. 검색어·분류·최대 가격 같은 쿼리 매개변수는 해당 상품 요청과 함께 전달됩니다. [NGINX location 규칙](https://nginx.org/en/docs/http/ngx_http_core_module.html#location)

`proxy_pass`에는 서비스 주소만 적고 경로를 덧붙이지 않았습니다. 따라서 `/api/products` 경로를 상품 앱까지 유지합니다. `proxy_pass`에 URI를 지정하는 경우에는 일치한 경로 부분을 치환하는 규칙이 적용되므로, API 경로를 그대로 넘길지 바꿀지에 따라 설정이 달라집니다. [NGINX proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)

## 로그인 상태가 유지되는 위치

브라우저는 NGINX 주소에 대해 세션 쿠키를 보관하고, NGINX는 로그인·장바구니·주문 요청을 주문 앱에 전달합니다. 세션 데이터는 주문 앱 한 인스턴스의 메모리에 있습니다. 같은 브라우저에서 화면만 새로고침하면 쿠키를 다시 보내므로 로그인 상태와 내 주문을 조회할 수 있습니다.

주문 앱을 재시작하면 메모리 세션이 사라져 다시 로그인해야 합니다. 주문과 회원은 DB에 남으므로 로그인 후 기존 주문을 조회할 수 있습니다. 상품 앱 재시작은 주문 앱의 메모리 세션을 지우지 않습니다.

같은 출처의 경로를 사용해도 변경 요청의 CSRF 검사는 그대로 적용합니다. 로그인한 상태에서 토큰 없이 주문 취소를 요청하면 403이며 주문 상태는 유지됩니다. 다른 회원의 주문 번호를 조회하거나 취소하면 404입니다.

## 내부 조회와 상태 검사 차단

```nginx
location = /internal { return 404; }
location ^~ /internal/ { return 404; }
location = /actuator { return 404; }
location ^~ /actuator/ { return 404; }
```

상품 앱의 내부 묶음 조회는 주문 앱에서만 사용하는 경로입니다. NGINX는 브라우저의 해당 요청을 상품 앱에 전달하지 않습니다. 주문 앱의 내부 HTTP 호출은 NGINX를 거치지 않으므로 계속 동작합니다. 기본 Compose에서는 두 앱과 두 DB에 호스트 포트도 연결하지 않습니다. 개발용 포트를 열면 그 포트는 NGINX의 경로 차단을 거치지 않으므로 IDE 실습을 마친 뒤 개발용 구성을 종료합니다.

## 로그와 장애 결과로 경로 확인하기

NGINX 로그의 `service`는 `proxy_pass` 대상 이름이고 `address`는 실제 연결한 주소입니다. 상품 요청의 `catalog_app`과 주문 요청의 `order_app`을 비교하면 같은 브라우저 주소에서 서로 다른 서비스로 요청이 전달되는 흐름을 볼 수 있습니다.

주문 앱을 중단해도 상품 앱이 정상이라면 상품 API는 200을 반환합니다. 상품 앱을 중단하면 상품 목록은 프록시 연결 상태에 따라 502 또는 504를 받지만, 주문 앱은 저장된 주문을 계속 조회합니다. 새 주문은 상품 값을 얻지 못해 503으로 끝납니다. 정적 HTML은 NGINX가 제공하므로 두 경우 모두 화면 파일 자체는 받을 수 있습니다.
