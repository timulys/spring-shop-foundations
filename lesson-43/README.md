# 43강 · 같은 주문 요청을 두 번 보내도 한 건만 만들기

브라우저는 하나의 상점 주소를 사용합니다. NGINX가 상품 API는 상품 서비스로, 로그인·회원·장바구니·주문 API는 주문 서비스로 전달합니다. 주문 앱은 요청 키로 중복 주문을 막고, 현재 상품을 조회할 때에는 두 시도와 대기 시간을 합쳐 2.5초의 시간 예산을 적용합니다.

## 실행

이전 강의의 앱과 컨테이너를 종료한 뒤 루트에서 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build --wait
docker compose ps
```

Windows에서는 `Copy-Item .env.example .env`로 복사할 수 있습니다. http://127.0.0.1:8080 에서 가입·로그인하고 키보드 2개와 마우스 1개를 주문하면 총액은 70,000원입니다. 기본 구성에서 호스트에 연결하는 서비스 포트는 NGINX의 8080뿐입니다.

브라우저에서 상품을 검색하고 로그인·주문을 진행한 뒤 로그를 확인해 주세요.

```bash
docker compose logs web
```

`GET /api/products` 행의 `service=catalog_app`, `POST /login`과 `POST /api/orders` 행의 `service=order_app`을 비교합니다. `address`는 실제 연결한 컨테이너 주소이며, `upstream_status`는 그 서비스가 반환한 상태입니다. 요청 본문·비밀번호·세션 쿠키는 이 로그 형식에 포함하지 않습니다.

## 주문 재전송과 장바구니 버전

주문 생성에는 `Idempotency-Key` 헤더가 필요합니다. 같은 회원이 같은 키와 본문으로 다시 요청하면 기존 주문 번호를 반환합니다. 키를 재사용하면서 본문을 바꾸면 409입니다. 주문과 요청 기록을 같은 DB 트랜잭션에 저장하므로 앱을 재시작해도 이 관계가 유지됩니다.

장바구니 응답은 `revision`을 포함합니다. 담기·수량 변경·삭제에는 마지막으로 받은 값을 `X-Cart-Revision` 헤더로 보냅니다. 다른 요청이 먼저 장바구니를 바꾸었으면 409로 알려 줍니다. 화면은 이 헤더를 자동으로 보내고, 응답을 받지 못한 주문의 키와 본문은 같은 탭의 회원별 `sessionStorage`에 보관합니다. `이전 주문 결과 다시 확인` 버튼은 저장된 요청을 다시 보냅니다.

주문 서비스 폴더에서 핵심 검사를 실행할 수 있습니다.

```bash
./gradlew test --tests study.shop.order.service.IdempotentOrderTest --tests study.shop.order.service.IdempotentOrderHttpTest
npm run test:ui
```

같은 키·다른 키의 동시 주문, 요청 조회와 장바구니 조회 사이의 경쟁, SQL 전송 후 롤백, 장바구니 버전 충돌을 검사합니다. [주문 키와 동시성](docs/idempotent-orders.md), [재시작 후 재전송 실습](docs/restart-replay.md)을 함께 읽어 주세요.

## 상품 조회의 실패 처리

연결 실패·시간 초과·502·503·504는 최대 두 번 시도합니다. 두 시도 사이에는 100ms를 기다립니다. 한 번의 HTTP 시도는 최대 1.5초이며, 두 번째 시도에는 전체 2.5초 중 남은 시간만 배정합니다. 잘못된 JSON·상품 값은 재시도하지 않고 503으로 안내합니다. 정상 응답에 요청한 상품이 없으면 404입니다.

상품 호출이 실패하면 새 주문과 장바구니 변경은 저장하지 않습니다. 과거 주문 조회·취소는 주문 DB의 스냅샷으로 처리하므로 상품 서비스가 중단되어도 사용할 수 있습니다.

주문 서비스 폴더에서 지연과 재시도 검사를 실행합니다.

```bash
./gradlew test --tests study.shop.order.infrastructure.CatalogRetryTest
```

검사는 응답 헤더가 늦는 경우와 본문만 늦는 경우를 나누어 실제 소요 시간을 출력합니다. 설정 범위·재시도 조건·측정 기준은 [원격 실패 처리](docs/remote-failures.md)에 있습니다.

## 검사

루트에서 테스트 DB를 시작합니다.

```bash
docker compose --profile test up -d --wait order-test-db catalog-test-db
```

- `catalog-service`: `./gradlew test bootJar` — Java 검사 23개.
- `order-service`: `./gradlew test bootJar` — Java 검사 225개.
- `order-service`: `npm run test:ui` — JavaScript 검사 20개.

각 명령은 해당 서비스 폴더에서 실행합니다. Windows에서는 `gradlew.bat`를 사용합니다. 테스트 DB의 호스트 포트는 `test` 프로필을 선택한 경우에만 사용합니다.

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, Spring Framework 7.0.10, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. IntelliJ에서는 각 서비스 폴더를 독립 Gradle 프로젝트로 열고 Project SDK와 Gradle JVM을 21로 지정합니다.

## 내부 경로와 개발용 포트

브라우저에서 `/internal/products/lookup`이나 `/actuator/health/readiness`를 요청하면 NGINX가 404를 반환합니다. 내부 상품 조회는 주문 앱이 컨테이너 네트워크에서 상품 앱으로 직접 호출합니다. 상태 검사는 컨테이너 안에서 실행합니다.

```bash
docker compose exec order curl -i http://127.0.0.1:8080/actuator/health/readiness
docker compose exec catalog curl -i http://127.0.0.1:8080/actuator/health/readiness
```

호스트 IDE에서 실행할 때만 `compose.dev.yaml`을 함께 사용해 DB 포트와 상품 앱의 18081 포트를 연결합니다. 먼저 중복되는 앱 컨테이너를 종료하고, IDE에 필요한 서비스만 시작합니다. 주문 앱을 IDE에서 실행하는 예는 다음과 같습니다.

```bash
docker compose stop order
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait order-db catalog
```

`ShopApplication`의 실행 환경에는 `SERVER_PORT=8082`, `DB_URL=jdbc:postgresql://127.0.0.1:15432/shop`, `DB_USER=shop`, `DB_PASSWORD=shop-local-only`, `CATALOG_BASE_URL=http://127.0.0.1:18081`을 지정합니다. 8082는 호스트에서 직접 주문 API를 확인하는 포트입니다. NGINX는 계속 컨테이너 이름 `order`를 사용하므로 IDE 앱의 8082로 자동 연결되지 않습니다. 같은 쇼핑 화면의 통합 실습은 전체 컨테이너 구성으로 진행합니다.

실습 후 사용한 Compose 파일과 프로필로 `down`을 실행합니다. 기본 구성은 `docker compose --profile test down`이며 DB 볼륨은 유지됩니다.

- [상품과 주문 요청의 전달 경로](docs/proxy-routing.md)
- [내부 상품 조회 계약](docs/catalog-lookup-contract.md)
