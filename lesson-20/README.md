# 20강 · 장바구니 수량 변경과 합계 표시

상품 카드에서 담기를 누르고, 장바구니에서 수량을 바꾸거나 삭제합니다. 서버가 반환한 수량·항목 금액·상품 합계를 화면에 표시합니다.

## 실행

JDK 21, Docker가 필요합니다. Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

http://127.0.0.1:8080/ 에서 시작합니다. IntelliJ에서는 이 폴더의 `build.gradle`을 열고 Project SDK와 Gradle JVM을 21로 선택한 뒤 `ShopApplication`을 실행합니다. 일반 DB 포트는 15432, 테스트 DB 포트는 15433입니다. 이전 강의 앱과 컨테이너를 종료한 뒤 실행해 주세요.

## 이번에 작성하는 코드

- `src/main/resources/static/cart.js`: 장바구니 조회·추가·수량 변경·삭제, 응답 표시와 실패 처리.
- `cart-input.js`: 수량 1~99 정수 검사.
- `shop.js`: 상품 카드의 담기 버튼, 상품 조회 뒤 장바구니 초기 조회.
- `ui-state.js`: 두 영역에서 공유하는 요청 상태와 입력 잠금.
- HTML·CSS는 장바구니 화면 틀로 제공하며, Java 서버와 `api.js`는 앞 강의에서 이어받습니다.

## 확인할 결과

빈 장바구니에서 시작합니다. 기존 항목이 있다면 화면에서 삭제한 뒤 따라 해 주세요.

| 조작 | 서버 응답과 화면 |
|---|---|
| 키보드 담기 두 번 | P001 한 행, 수량 2, 60000원 |
| 마우스 담기 한 번 | 총 수량 3, 상품 합계 75000원 |
| 키보드 수량을 3으로 변경 | 키보드 90000원, 총 수량 4, 합계 105000원 |
| 수량 100 입력 후 변경 | 화면에서 거부하고 마지막 조회 수량으로 복구 |
| 직접 PATCH로 수량 100 전송 | 서버 400, DB 수량과 합계 유지 |
| 키보드 삭제 | 마우스 한 행, 수량 1, 합계 15000원 |
| 모든 항목 삭제 | 빈 안내, 총 수량 0, 상품 합계 0원 |
| 새로고침 | DB에 저장된 장바구니를 다시 조회 |

상품 합계에는 주문 할인이 아직 적용되지 않습니다. 주문 확인은 21강에서 연결합니다. 요청 본문에는 상품 코드와 수량만 보내며, 단가·항목 금액·합계는 서버 응답을 사용합니다.

HTTP 오류가 나면 마지막으로 받은 장바구니를 다시 표시합니다. 연결이 끊겨 응답을 못 받은 경우에는 서버 처리 여부가 화면만으로 확인되지 않으므로, 장바구니 새로고침으로 현재 상태를 조회합니다. 변경 요청을 자동 재전송하지 않습니다.

현재 장바구니는 고정된 학습용 사용자 1번의 로컬 실습 데이터입니다. 회원별 분리는 30~33강에서 다룹니다.

## 코드 위치

- `src/main/resources/static/cart.js`
- `src/main/resources/static/cart-input.js`
- `src/main/resources/static/shop.js`
- `src/main/resources/static/ui-state.js`
- `src/main/java/study/shop/CartController.java`
- `src/main/java/study/shop/CartService.java`
- `src/main/java/study/shop/CartResponse.java`

수량 100을 입력했을 때 오류 문구와 기존 수량이 함께 표시되도록 직접 작성해 보세요. 정상 수량으로 다시 변경할 수 있어야 합니다.
