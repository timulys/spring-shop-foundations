# 14강 · 변경 감지로 주문 취소하기

트랜잭션 안에서 조회한 주문의 상태를 `CONFIRMED`에서 `CANCELLED`로 바꿉니다. 관리 상태의 엔티티는 변경 감지 대상이므로, 서비스에서 `save()`를 다시 호출하지 않아도 커밋 전에 변경 내용이 DB에 반영됩니다.

## 실행

JDK 21, Docker가 필요합니다. Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을 21로 지정해 주세요. 이전 강의의 앱과 DB 컨테이너를 종료합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

앱은 `http://localhost:8080`, DB는 `127.0.0.1:15432`입니다. 다른 포트는 `POSTGRES_PORT=15444 docker compose up -d --wait`와 앱의 `DB_URL=jdbc:postgresql://127.0.0.1:15444/shop`을 함께 사용합니다. 강의별 DB 볼륨은 분리되어 있습니다.

## 직접 작성할 부분

1. `ShopOrder.cancel()`에서 확정 상태를 검사하고 취소 상태로 바꿉니다.
2. `OrderService.cancel()`에서 주문 조회·상태 변경·DTO 변환을 하나의 트랜잭션으로 묶습니다.
3. 컨트롤러의 `POST /api/orders/{id}/cancel`을 연결합니다.

`OrderRepository.findForUpdate()`는 제공된 잠금 조회입니다. 같은 주문에 취소 요청이 겹치면 차례로 상태를 검사하게 합니다. 잠금은 트랜잭션이 끝날 때 풀립니다.

## 주문하고 취소하기

먼저 `GET /api/cart`로 장바구니를 확인합니다. 기존 항목은 `DELETE /api/cart/items/{상품코드}`로 비운 뒤 진행해 주세요.

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' -d '{"productCode":"P001","quantity":2}'
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' -d '{"productCode":"P002","quantity":1}'
curl -i -X POST http://localhost:8080/api/orders
```

응답으로 받은 주문 번호를 아래 `1` 대신 넣습니다.

```bash
curl -i -X POST http://localhost:8080/api/orders/1/cancel
curl -s http://localhost:8080/api/orders/1
curl -i -X POST http://localhost:8080/api/orders/1/cancel
```

첫 취소는 `200`, 상태는 `CANCELLED`입니다. 주문 항목 2개와 주문 당시 총액 70000원은 유지됩니다. 같은 주문을 다시 취소하면 `409 ORDER_STATE_CONFLICT`, 없는 주문이면 `404 ORDER_NOT_FOUND`입니다. 취소한 상품을 장바구니에 다시 넣지는 않습니다. 이 과정의 결제는 `SIMULATED` 모의 승인으로 실제 청구·환불은 없습니다.

## 변경 감지·분리·롤백 비교

앱을 종료하고 DB만 실행한 상태에서 테스트합니다.

```bash
./gradlew test
./gradlew test --tests study.shop.OrderCancellationTest --rerun-tasks
```

전체 28개 테스트 가운데 `OrderCancellationTest`의 8개가 이번 강의 내용입니다.

| 조건 | 객체의 상태 | 트랜잭션 종료 후 DB |
|---|---|---|
| 관리 상태에서 취소 후 정상 커밋 | CANCELLED | CANCELLED |
| detach 후 객체만 취소 | CANCELLED | CONFIRMED |
| 관리 상태에서 취소·flush 후 예외로 롤백 | CANCELLED까지 변경 | CONFIRMED |

`flush()`는 변경 내용을 SQL로 DB에 동기화합니다. 커밋 전에 예외로 롤백하면 실행된 UPDATE도 취소됩니다. `detach()`로 분리한 객체의 이후 변경은 해당 영속성 컨텍스트에서 추적하지 않습니다. 취소 로직은 읽기 전용이 아닌 쓰기 트랜잭션에서 실행합니다.

테스트는 자신이 만든 주문만 정리하며 이전 주문은 유지합니다. 상품 가격은 기본값인 키보드 30000원·마우스 15000원이어야 합니다. 테스트와 앱 요청을 동시에 실행하지 마세요.

## 과제

`ShopOrder.cancel()`의 상태 검사를 잠시 제거한 뒤 반복 취소 테스트를 실행해 보세요. 두 번째 요청도 성공해 `409` 검사가 실패합니다. 조건문을 복원하고 전체 테스트를 통과시키세요.

앱은 `Ctrl+C`, DB는 `docker compose down`으로 종료합니다. 볼륨은 유지됩니다.
