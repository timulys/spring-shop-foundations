# 13강 · 주문 저장과 장바구니 비우기를 함께 처리하기

주문이 만들어지면 장바구니를 비웁니다. 같은 DB 트랜잭션 안에서 주문·주문 항목 저장과 장바구니 항목 삭제를 처리하므로, 롤백 조건에 해당하는 예외가 발생하면 이 변경들이 함께 취소됩니다.

## 실행

JDK 21, Docker가 필요합니다. Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을 21로 지정해 주세요.

이전 강의의 앱과 DB 컨테이너를 종료한 뒤 실행합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

앱은 `http://localhost:8080`, DB는 `127.0.0.1:15432`입니다. 다른 포트가 필요하면 `POSTGRES_PORT=15443 docker compose up -d --wait`로 실행하고 앱에도 `DB_URL=jdbc:postgresql://127.0.0.1:15443/shop`을 전달합니다. 강의별 DB 볼륨이 분리되어 있습니다.

## 직접 작성할 부분

1. `Cart.clear()`에서 각 항목의 부모 참조와 장바구니 목록을 정리합니다.
2. `OrderService.place()`의 `orders.save(order)` 다음에 `cart.clear()`를 넣습니다.
3. 두 동작이 같은 `@Transactional` 메서드 안에 있는지 확인합니다.

`orphanRemoval = true`가 있으므로 이미 저장된 장바구니 항목이 목록에서 빠지면 삭제 대상이 됩니다. 부모 장바구니와 상품 데이터는 유지됩니다. 주문 항목은 별도로 복사한 데이터이므로 장바구니를 비워도 남습니다.

## 정상 주문

장바구니를 먼저 조회하고, 앞선 실습 항목이 있으면 `DELETE /api/cart/items/{상품코드}`로 비운 뒤 시작합니다.

```bash
curl -s http://localhost:8080/api/cart
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":2}'
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P002","quantity":1}'
curl -i -X POST http://localhost:8080/api/orders
curl -s http://localhost:8080/api/cart
```

주문 응답은 `201 Created`, 총액은 70000원입니다. 이후 장바구니의 `items`는 빈 배열이고 수량과 소계는 0입니다. 주문 응답에서 받은 번호로 `GET /api/orders/{주문번호}`를 호출하면 두 주문 항목이 남아 있습니다. 주문 요청을 바로 다시 보내면 `400 EMPTY_CART`이며 새 주문을 저장하지 않습니다.

학습용 장바구니 1번과 `SIMULATED` 모의 승인을 사용합니다. 실제 결제·배송은 하지 않습니다. 이 빈 장바구니 검사는 재전송 전체에 대한 중복 방지 설계가 아닙니다. 멱등성 처리는 후속 강의에서 다룹니다.

## 실제 롤백과 체크 예외 비교

앱을 종료하고 DB만 켠 상태에서 실행해 주세요.

```bash
./gradlew test
./gradlew test --tests study.shop.OrderTransactionTest --rerun-tasks
```

전체 테스트는 20개입니다. `OrderTransactionTest`의 5개 검사는 실제 PostgreSQL에서 아래 결과를 확인합니다.

| 상황 | 주문·주문 항목 | 장바구니 항목 |
|---|---|---|
| 정상 종료 | 주문 1개와 항목 2개 저장 | 0개 |
| flush 뒤 RuntimeException | 새 주문과 항목 없음 | 기존 2개 유지 |
| 기본 설정에서 체크 예외 | 새 주문 저장 | 0개 |
| rollbackFor로 지정한 체크 예외 | 새 주문과 항목 없음 | 기존 2개 유지 |
| 성공 직후 다시 주문 | EMPTY_CART, 추가 주문 없음 | 0개 |

실패를 일으키는 빈은 테스트 소스에만 있습니다. 테스트가 그 빈의 프록시를 호출하면 트랜잭션이 시작되고, 내부의 `OrderService.place()`는 같은 트랜잭션에 참여합니다. `flush()`로 SQL을 DB에 보낸 뒤 예외를 던져도 커밋 전에 롤백할 수 있습니다. 테스트 메서드에는 `@Transactional`을 붙이지 않아 프록시 호출이 끝난 뒤 실제 커밋·롤백 결과를 별도 조회로 확인합니다.

Spring의 기본 설정은 `RuntimeException`과 `Error`에 롤백을 적용합니다. 체크 예외를 롤백 대상으로 추가하려면 예제처럼 `rollbackFor = CheckedFailure.class`를 지정합니다. 기본 프록시 방식에서는 같은 객체 안에서 자기 메서드를 호출하는 것만으로 새 트랜잭션 설정이 적용되지 않습니다. 이 프로젝트는 Spring이 주입한 별도 빈을 통해 호출합니다.

테스트가 만든 주문만 번호로 정리하고, 실행 전 장바구니의 상품 코드와 수량을 복원합니다. 테스트와 앱 요청을 동시에 실행하지 마세요. 상품 가격을 수동으로 바꿨다면 기본 키보드 30000원·마우스 15000원으로 복원한 뒤 실행합니다.

## 과제

`failWithCheckedRollbackRule()`의 `rollbackFor` 설정을 잠시 제거하면 어떤 검사가 실패할지 예상해 보세요. 주문은 저장되고 장바구니는 비워지므로, 기존 항목이 유지되어야 한다는 검사가 실패합니다. 확인 후 설정을 복원하고 전체 테스트를 다시 실행합니다.

앱은 `Ctrl+C`, DB 컨테이너는 `docker compose down`으로 종료합니다. 볼륨은 유지됩니다.
