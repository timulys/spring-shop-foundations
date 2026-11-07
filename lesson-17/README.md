# 17강 · HTTP와 DB를 함께 검증하는 테스트

실제 HTTP 서버로 주문을 요청하고 응답과 PostgreSQL에 저장된 결과를 함께 확인합니다. 16강의 앱 기능을 유지하고 테스트 전용 DB와 통합 테스트를 추가했습니다.

Java 21, Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을 21로 지정합니다.

## 실행

Docker 실행 환경을 먼저 켜 주세요. 테스트만 실행할 때는 앱을 따로 시작할 필요가 없습니다.

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
```

테스트는 15433 포트의 `shop_test`에 연결합니다. 전체 53개 중 새 HTTP 통합 테스트는 10개입니다. 통합 테스트가 임의의 빈 포트에서 서버를 실행하고, 각 테스트 전에 전용 DB의 주문과 장바구니 데이터를 정리합니다. 각 테스트는 필요한 상품과 수량을 직접 준비합니다.

```bash
./gradlew test --tests study.shop.ShopApiIntegrationTest --rerun-tasks
TEST_ORDER_SEED=42 ./gradlew test --tests study.shop.ShopApiIntegrationTest --rerun-tasks
```

`TEST_ORDER_SEED`를 바꾸면 메서드 실행 순서가 달라집니다. 순서가 바뀌어도 결과가 같아야 합니다. 동시에 여러 테스트 실행을 시작하면 같은 테스트 DB를 공유하므로 한 번의 실행이 끝난 뒤 다음 실행을 시작합니다.

일반 앱은 기존처럼 별도의 실습 DB `shop`을 사용합니다.

```bash
docker compose up -d --wait db
./gradlew bootRun
```

앱 주소는 `http://127.0.0.1:8080`, 상품 목록은 `/api/products`입니다. 앱의 DB 포트는 15432이며 `postgres_data` 볼륨에 저장합니다. 테스트 DB는 임시 메모리 저장소를 사용하며 컨테이너가 정지하면 데이터가 사라집니다. 테스트 DB를 시작하면 Flyway가 테이블과 기본 상품을 준비합니다.

포트를 바꾸려면 앱 DB는 `POSTGRES_PORT`와 `DB_URL`, 테스트 DB는 `TEST_POSTGRES_PORT`와 `TEST_DB_URL`을 각각 지정합니다. 테스트 접속 URL의 데이터베이스 이름은 `shop_test`로 유지합니다.

```bash
TEST_POSTGRES_PORT=15477 docker compose --profile test up -d --wait test-db
TEST_DB_URL=jdbc:postgresql://127.0.0.1:15477/shop_test ./gradlew test
```

## 직접 작성하고 확인할 부분

`src/test/java/study/shop/ShopApiIntegrationTest.java`에서 요청 뒤의 상태 코드, JSON 본문, DB 값을 검사하는 부분을 작성해 보세요. JDK HTTP 클라이언트로 요청하는 도구와 데이터 준비·정리 코드는 함께 제공됩니다.

| 조건 | 기대 응답과 저장 결과 |
|---|---|
| 키보드 2개 + 마우스 1개 주문 | 201, 총액 70000, 주문 항목 2개, 장바구니 0개 |
| 소계 49999 | 할인 0, 배송비 3000, 총액 52999 |
| 소계 50000 | 할인 5000, 배송비 0, 총액 45000 |
| 수량 0 또는 100 | 400 VALIDATION_ERROR, 장바구니 저장 0건 |
| 소수 수량 1.5 | 400 INVALID_JSON |
| 미등록 상품 | 404 PRODUCT_NOT_FOUND |
| 빈 장바구니 주문 | 400 EMPTY_CART, 주문 저장 0건 |
| 이미 취소한 주문 재취소 | 409 ORDER_STATE_CONFLICT, DB는 CANCELLED 유지 |
| 테스트 전용 경로에서 flush 뒤 실패 | 500 INTERNAL_ERROR, 주문 0건, 기존 장바구니 유지 |

`RANDOM_PORT`의 서버 요청은 테스트 메서드와 다른 스레드의 트랜잭션에서 실행됩니다. 따라서 이 클래스는 각 테스트의 데이터 준비와 정리를 명시적으로 수행합니다. `@Transactional`을 테스트에 붙이는 것만으로 HTTP 서버의 커밋이 자동 복구되지는 않습니다.

`HttpFailureTestConfiguration`은 `src/test`의 제공 도구입니다. 테스트 중에만 `/test/orders/fail-after-flush`를 열어 실제 SQL 반영 후 실패와 롤백을 재현합니다. 일반 앱과 `bootJar`에는 이 설정과 경로가 포함되지 않습니다. 주문은 학습용 모의 승인으로 처리하며 실제 결제 서버를 호출하지 않습니다.

## 과제

미등록 상품을 담는 요청과 빈 장바구니 주문의 테스트를 직접 작성해 보세요. 오류 코드와 상태 코드에 더해 장바구니 또는 주문 행이 추가되지 않았는지도 검사합니다. 완성 코드의 두 테스트와 비교한 뒤 메서드 실행 순서를 바꿔 다시 실행합니다.

실습 종료 시 앱을 먼저 종료하고 컨테이너를 정지합니다.

```bash
docker compose --profile test down
```
