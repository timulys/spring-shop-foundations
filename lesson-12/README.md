# 12강 · 주문 당시 상품 정보를 보관하기

장바구니의 상품을 주문으로 저장합니다. 주문 항목에는 당시 상품 코드·이름·단가·수량을 복사하므로 현재 상품을 변경해도 이전 주문의 정보와 금액이 유지됩니다.

## 실행

JDK 21, Docker 실행 환경이 필요합니다. Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더의 `build.gradle`을 열고 Project SDK와 Gradle JVM을 21로 지정해 주세요.

이전 강의의 앱과 DB 컨테이너를 종료한 뒤 이 폴더에서 실행합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

앱은 `http://localhost:8080`, DB는 `127.0.0.1:15432`를 사용합니다. 다른 DB 포트가 필요하면 `POSTGRES_PORT=15442 docker compose up -d --wait`로 실행하고 앱에도 `DB_URL=jdbc:postgresql://127.0.0.1:15442/shop`을 전달합니다. 강의마다 별도 DB 볼륨을 사용합니다. 이 강의의 첫 실행에서는 상품 세 개와 빈 학습용 장바구니 1번을 만듭니다.

## 직접 작성할 부분

1. `OrderItem.java` 생성자에서 상품 코드·이름·단가와 요청 수량을 필드에 복사합니다.
2. `ShopOrder.java`의 `confirm()`에서 소계, 할인, 배송비, 최종 금액을 계산합니다.
3. `@Enumerated(EnumType.STRING)`을 확인하고 DB의 `status` 값과 비교합니다.

테이블 생성문, 컨트롤러, 저장소와 트랜잭션 연결은 완성 코드로 제공합니다. 주문 생성과 상세 조회만 추가하며 웹 화면의 주문 버튼은 후속 강의에서 연결합니다.

## 주문 생성과 조회

먼저 `GET /api/cart`로 현재 항목을 확인해 주세요. 이미 연습한 항목이 있으면 `DELETE /api/cart/items/{상품코드}`로 해당 항목을 비운 뒤 아래 요청을 보냅니다.

```bash
curl -s http://localhost:8080/api/cart
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":2}'
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P002","quantity":1}'
curl -i -X POST http://localhost:8080/api/orders
```

`201 Created`와 주문 번호가 나옵니다. 상품 합계는 75000원, 할인은 5000원, 배송비는 0원, 최종 금액은 70000원입니다. `status`는 `CONFIRMED`, `paymentMode`는 `SIMULATED`입니다. 실제 결제나 배송을 요청하지 않는 학습용 모의 승인입니다.

응답의 `id`를 아래 경로에 넣습니다. 자동 생성 번호는 테스트나 이전 실습에 따라 달라집니다.

```bash
curl -s http://localhost:8080/api/orders/주문번호
```

이 강의에서는 주문 뒤 장바구니가 유지됩니다. 주문 요청을 다시 보내면 새 주문이 하나 더 생깁니다. 다음 13강에서 주문 저장과 장바구니 비우기를 함께 처리하고, 요청 재전송의 중복 방지는 후속 강의에서 추가합니다.

## 현재 상품을 바꾼 뒤 이전 주문 확인

학습용 DB에서 키보드의 이름과 가격을 바꿉니다.

```bash
docker compose exec -T db psql -U shop -d shop -c \
  "UPDATE products SET name='새 키보드', unit_price_won=40000 WHERE code='P001';"
curl -s http://localhost:8080/api/products/P001
curl -s http://localhost:8080/api/orders/주문번호
```

현재 상품은 새 키보드·40000원으로 나오지만 이전 주문 항목은 키보드·30000원이고 주문 총액은 70000원으로 유지됩니다. 장바구니는 현재 상품 가격으로 계산하므로 소계가 95000원이 됩니다. 같은 장바구니로 새 주문을 만들면 새 가격을 적용한 최종 금액 90000원이 나옵니다.

실습 후 현재 상품을 기본값으로 돌리려면 다음 명령을 실행합니다. 기존 주문 스냅샷에는 영향을 주지 않습니다.

```bash
docker compose exec -T db psql -U shop -d shop -c \
  "UPDATE products SET name='키보드', unit_price_won=30000 WHERE code='P001';"
```

## 계산 규칙과 오류

- 상품 합계가 50000원 이상이면 5000원을 할인하고 배송비는 0원입니다.
- 상품 합계가 50000원보다 작으면 할인은 0원, 배송비는 3000원입니다.
- 빈 장바구니는 `400 EMPTY_CART`, 현재 상품이 없으면 `404 PRODUCT_NOT_FOUND`, 없는 주문은 `404 ORDER_NOT_FOUND`입니다.
- 상품별 수량은 1~99개, 서로 다른 상품은 최대 100종류입니다. 금액은 원 단위 `long`으로 계산합니다.
- 주문 조회에는 저장된 항목을 사용합니다. 현재 상품과 조인해 주문 가격을 다시 계산하지 않습니다.
- 로그인 도입 전 고정 학습용 사용자 범위입니다. 회원별 접근 제한은 후속 강의에서 다룹니다.

## 테스트와 종료

DB를 켜 둔 상태에서 `./gradlew test`를 실행합니다. 기존 검사 7개와 주문 검사 8개가 있습니다. DB 테스트가 만든 주문과 상품 변경은 테스트 종료 시 롤백합니다. 기본 상품을 수동으로 바꾼 상태라면 위 복원 명령을 먼저 실행해 주세요.

앱은 실행 터미널에서 `Ctrl+C`로 종료하고 `docker compose down`으로 컨테이너를 내립니다. 볼륨은 유지되므로 다음 실행에서도 실습 데이터가 남습니다.
