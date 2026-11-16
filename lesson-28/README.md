# 28강 · 상품 조회 계약과 구현 분리

주문 서비스는 `ProductLookup`으로 상품을 조회합니다. DB 구현은 상품 엔티티를 주문에 필요한 `ProductSnapshot`으로 바꾸고, 테스트 구현도 같은 값을 반환합니다.

## 실행

JDK 21, Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

Windows에서는 `gradlew.bat`를 사용합니다. IntelliJ에서 build.gradle을 열고 Project SDK와 Gradle JVM을 21로 지정합니다. 일반 DB는 15432, 테스트 DB는 15433, 상점 주소는 http://127.0.0.1:8080/ 입니다. 이전 강의의 앱과 컨테이너를 종료한 뒤 실행해 주세요. Node.js 22 환경의 `npm run test:ui`는 요청 함수 테스트 6개를 실행합니다.

## 코드를 읽는 순서

1. `src/main/java/study/shop/order/service/port/ProductLookup.java`: 코드로 조회하고 Optional로 존재 여부를 전달하는 계약
2. `src/main/java/study/shop/order/domain/ProductSnapshot.java`: 코드·이름·Money 단가를 담는 주문의 불변 입력값
3. `src/main/java/study/shop/order/infrastructure/DatabaseProductLookup.java`: 상품 저장소와 주문 계약을 연결하는 구현
4. `src/main/java/study/shop/order/service/OrderService.java`: 생성자로 계약을 받고 기존 조회·확정·저장 흐름 유지
5. `src/test/java/study/shop/order/service/FakeProductLookupFlowTest.java`: 고정 조회 구현으로 같은 서비스 실행

스냅샷은 JPA 엔티티가 아닙니다. 주문 항목 생성에도 사용하므로 order.domain에 두어 도메인이 서비스나 catalog 엔티티를 참조하지 않게 합니다. 이 강의의 포트는 같은 프로세스 안에서 호출하는 Java 인터페이스입니다. HTTP 통신은 이후 서비스 분리에서 다룹니다.

## 확인할 결과

전체 Java 테스트 106개, 요청 함수 테스트 6개가 통과합니다. 기존 99개에 스냅샷 2개, DB 구현 2개, 가짜 구현 흐름 3개를 추가했습니다. 키보드 2개와 마우스 1개는 기본 정책에서 최종 70000원입니다. 테스트에서 DB 단가를 990000원으로 바꾸어도 고정 조회 구현의 단가 30000원이 주문에 들어갑니다. 테스트 트랜잭션이 끝나면 DB 변경은 롤백됩니다.

미리보기 뒤 가짜 조회 목록에서 P002를 제거하면 주문 요청은 PRODUCT_NOT_FOUND로 실패합니다. 주문 수와 기존 장바구니 항목 2개가 유지됩니다. cart와 주문 저장은 실제 테스트 DB를 사용하며 상품 조회 부분만 교체합니다.

```bash
./gradlew test --tests study.shop.order.service.FakeProductLookupFlowTest
```

자세한 의존 방향과 실습 결과는 [상품 조회 계약](docs/product-lookup.md)을 참고하세요. 로그인 도입 전 학습용 상점이며 결제·취소는 실제 청구나 환불 없는 모의 처리입니다.
