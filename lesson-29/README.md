# 29강 · 리팩터링 전후를 같은 시나리오로 비교하기

기본 구매 결과를 유지하면서 파일과 객체의 역할을 비교합니다. [비교 기준과 정책 과제](docs/refactoring-comparison.md)를 먼저 읽어 주세요.

## 실행

JDK 21, Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

Windows에서는 `gradlew.bat`를 사용합니다. IntelliJ에서 build.gradle을 열고 Project SDK와 Gradle JVM을 21로 지정합니다. 일반 DB는 15432, 테스트 DB는 15433, 상점 주소는 http://127.0.0.1:8080/ 입니다. 이전 강의의 앱과 컨테이너를 종료한 뒤 실행해 주세요. Node.js 22 환경의 `npm run test:ui`는 요청 함수 테스트 6개를 실행합니다.

## 코드를 읽는 순서

1. [전후 비교 기준](docs/refactoring-comparison.md): 같은 입력·초기 데이터로 비교할 응답과 저장 결과
2. `src/main/java/study/shop/order/service/OrderService.java`: 조회·충돌 검사·저장·장바구니 비우기를 잇는 흐름
3. `src/main/java/study/shop/order/domain/ShopOrder.java`: 주문 상태와 항목 변경 규칙
4. `src/main/java/study/shop/order/domain/DiscountPolicy.java`: 상품 합계를 받아 할인액을 반환하는 계약
5. `src/main/java/study/shop/order/domain/TenPercentDiscountPolicy.java`: 원 미만을 버리는 10% 할인 구현
6. `src/test/java/study/shop/order/domain/TenPercentDiscountPolicyTest.java`: 금액·배송비·자료형 경계 검사

## 확인할 결과

전체 Java 테스트 111개와 요청 함수 테스트 6개가 통과합니다. 28강의 106개에 새 할인 정책 검사 5개를 추가했습니다. 기본 앱은 기존 정액 할인 정책을 사용하므로 키보드 2개와 마우스 1개의 최종 금액은 70,000원입니다. 새 정책을 전달한 테스트 주문은 할인액 7,500원, 배송비 0원, 최종 금액 67,500원입니다.

```bash
./gradlew test --tests study.shop.order.domain.TenPercentDiscountPolicyTest
npm run test:ui
```

23강과 이 강의를 각각 별도의 데이터베이스에서 실행하여 같은 주문과 취소를 수행해 보세요. 상품 단가 49,999원과 50,000원의 기본 정책 결과는 각각 52,999원과 45,000원입니다. 주문 후 상품 가격을 바꾸어도 기존 주문의 단가와 총액은 유지됩니다. 취소 후에도 항목과 금액이 남고, 다시 취소하면 409 오류가 발생합니다.

로그인 도입 전 학습용 상점입니다. 결제·취소는 실제 청구나 환불 없이 처리합니다. 새 정책은 단위 테스트에서 선택하며 기본 앱의 빈으로 등록하지 않습니다.
