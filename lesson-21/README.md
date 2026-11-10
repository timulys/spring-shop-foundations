# 21강 · 주문 확인과 모의 결제 화면

서버가 계산한 상품 합계·할인·배송비·최종 금액을 확인한 뒤 학습용 주문을 만듭니다. 실제 결제 서버를 호출하거나 금액을 청구하지 않습니다.

## 실행

JDK 21, Docker가 필요합니다. Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. http://127.0.0.1:8080/ 에서 시작합니다. IntelliJ에서는 이 폴더의 build.gradle을 열고 Project SDK와 Gradle JVM을 21로 지정한 뒤 ShopApplication을 실행합니다. 일반 DB 포트는 15432, 테스트 DB 포트는 15433입니다. 이전 강의 앱과 컨테이너를 종료한 뒤 실행해 주세요.

## 작성할 코드

- `src/main/resources/static/checkout.js`: 주문 확인 조회, 응답 표시, 모의 결제 요청과 완료 표시.
- `ui-state.js`와 `cart.js`: 요청 상태와 장바구니 변경 알림.
- `src/main/java/study/shop/OrderService.java`: 확인 금액 계산과 주문 시 재계산·비교.
- `OrderController.java`, `PlaceOrderRequest.java`, `OrderPreviewResponse.java`: 주문 확인 API 계약.
- HTML·CSS, JSON 정수 검사, V4 장바구니 버전 마이그레이션은 제공 파일입니다. Cart는 변경할 때 revision을 하나 늘립니다.

## 확인할 결과

키보드 2개와 마우스 1개를 담고 주문 내용 확인을 누릅니다. 상품 합계 75000원, 할인 5000원, 배송비 0원, 최종 금액 70000원이 표시됩니다. 확인 조회만으로 주문은 저장되지 않습니다. 모의 결제를 누르면 주문 번호와 70000원이 표시되고 장바구니가 비워집니다.

`GET /api/orders/preview`는 cartRevision과 서버 계산 금액을 반환합니다. `POST /api/orders`에는 확인한 `cartRevision`, `expectedTotalWon`을 JSON 정수로 보냅니다. 서버는 장바구니를 잠그고 상품 가격을 다시 읽어 계산합니다. 버전이 다르면 409 CART_CHANGED, 금액이 다르면 409 PRICE_CHANGED로 새 확인을 요구합니다. 오류 응답에서는 주문을 저장하거나 장바구니를 비우지 않습니다.

가격 변경 시나리오는 `CheckoutHttpTest`의 `priceChangeRequiresNewConfirmationAndLeavesCartIntact`에서 실행할 수 있습니다. 확인 후 키보드 단가를 31000원으로 바꾸면 기존 금액의 주문은 409가 되고, 새 확인 금액 72000원으로 주문하면 성공합니다. 테스트는 shop_test 전용 DB에서 실행하며 일반 실습 DB와 분리됩니다.

연결이 끊겨 주문 응답을 받지 못하면 서버에서 주문이 만들어졌을 수도 있습니다. 현재 주문과 장바구니를 조회해 결과를 확인한 뒤 진행합니다. 화면은 주문 요청을 자동으로 재전송하지 않습니다. 주문 내역 화면은 22강에서 이어 만듭니다.

현재는 고정된 학습용 사용자 1번의 장바구니입니다. 사용자 구분은 30~33강, 요청 중복 방지와 동시성의 확장은 이후 강의에서 다룹니다.
