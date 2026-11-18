# 41강 · 같은 쇼핑 화면을 두 서비스에 연결하기

브라우저는 하나의 상점 주소를 사용합니다. NGINX가 상품 API는 상품 서비스로, 로그인·회원·장바구니·주문 API는 주문 서비스로 전달합니다. 주문 앱의 임시 상품 전달 코드는 제거했습니다.

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

## 검사

루트에서 테스트 DB를 시작합니다.

```bash
docker compose --profile test up -d --wait order-test-db catalog-test-db
```

- `catalog-service`: `./gradlew test bootJar` — Java 검사 23개.
- `order-service`: `./gradlew test bootJar` — Java 검사 190개.
- `order-service`: `npm run test:ui` — JavaScript 검사 10개.

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
