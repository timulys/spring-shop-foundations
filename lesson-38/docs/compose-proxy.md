# 브라우저 주소 하나로 상점 이용하기

브라우저는 NGINX에 화면과 API를 요청합니다. NGINX는 HTML·CSS·JavaScript를 파일로 응답하고, `/api/`·`/login`·`/logout` 요청은 앱으로 전달합니다. 앱이 돌려준 상태 코드와 JSON, 세션 쿠키도 브라우저에 전달합니다.

## 요청이 이동하는 주소

| 요청 구간 | 주소 |
|---|---|
| 브라우저 → NGINX | 127.0.0.1:8080 |
| NGINX → Spring 앱 | app:8080 |
| Spring 앱 → PostgreSQL | db:5432 |

Compose는 서비스들이 공유하는 네트워크를 만들고 서비스 이름을 내부 주소로 찾을 수 있게 합니다. 컨테이너끼리 통신할 때는 내부 포트를 사용합니다. 호스트에 공개한 포트는 호스트에서 컨테이너에 들어오는 연결에 사용합니다. [Compose 네트워크](https://docs.docker.com/compose/how-tos/networking/)

앱 컨테이너에서 `127.0.0.1`은 앱 컨테이너 자신입니다. DB 컨테이너를 찾으려면 `db`를 사용합니다. 호스트의 IntelliJ에서 실행하는 앱은 개발용으로 연결한 `127.0.0.1:15432`를 사용합니다.

## 경로와 로그인 상태

`proxy_pass http://shop_app;`에는 새 URI를 덧붙이지 않으므로 `/api/products?q=...`의 경로와 쿼리를 앱에 그대로 보냅니다. `Host`와 전달 주소 헤더는 `nginx/proxy.conf`에 모았습니다. 앱은 `server.forward-headers-strategy=framework` 설정으로 프록시가 전달한 원래 요청 정보를 해석합니다. [NGINX proxy_pass](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)

화면의 요청 함수는 `/api/...` 같은 상대 경로를 사용합니다. 화면 주소와 API 주소의 스킴·호스트·포트가 같으므로 브라우저에서 하나의 출처로 처리합니다. `/api/csrf`에서 받은 토큰을 로그인·변경 요청에 넣고, 같은 출처의 세션 쿠키를 함께 보냅니다. 다른 출처를 허용하는 CORS 설정은 이 구성에 필요하지 않습니다.

앱의 Spring Security가 로그인과 CSRF를 검사하고, 회원 조건을 포함한 저장소 쿼리가 주문 접근 범위를 결정합니다. 따라서 같은 주소를 거쳐도 다른 회원의 주문은 404를 받습니다.

## 앱 주소가 바뀔 때

컨테이너를 다시 만들면 내부 IP가 바뀔 수 있습니다. NGINX의 `resolve` 설정은 서비스 이름을 다시 조회하고, `zone`은 그 서버 정보를 공유할 공간을 만듭니다. 이 예제는 Docker 내장 DNS인 `127.0.0.11`에서 주소를 찾고 5초 동안 캐시합니다. 프록시 컨테이너를 유지한 채 앱의 내부 IP가 바뀌어도 주소를 다시 찾아 요청할 수 있습니다. [Docker 내장 DNS](https://docs.docker.com/engine/network/), [NGINX upstream resolve](https://nginx.org/en/docs/http/ngx_http_upstream_module.html)

NGINX가 앱의 주소를 찾아도 앱 초기화가 끝나기 전에는 API 요청이 실패할 수 있습니다. 현재 37강에서는 상태 검사와 시작 조건을 적용해 준비 완료를 기다립니다.

## 데이터와 세션의 수명

앱은 실행 중인 메모리에 로그인 상태를 보관합니다. 앱을 다시 만들면 새 로그인 세션이 필요합니다. 회원·장바구니·주문은 PostgreSQL의 볼륨에 저장되므로 다시 로그인한 뒤 기존 데이터를 조회할 수 있습니다. 36강에서는 기존 데이터를 보존한 채 취소 시각 열을 추가합니다.
