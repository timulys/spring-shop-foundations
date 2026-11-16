# 27강 · 할인 정책과 애플리케이션 흐름 분리하기

같은 주문을 기본 할인 정책과 할인 없음 정책으로 계산합니다. DiscountPolicy와 두 구현은 order.domain에 두고, OrderService는 조회·검증·저장 순서를 이어갑니다. 기존 자바 과정의 calculateDiscount 계약을 Money 입출력으로 옮겼습니다.

## 실행

JDK 21과 Docker가 필요합니다. Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. IntelliJ에서 build.gradle을 열고 Project SDK와 Gradle JVM을 21로 지정합니다. http://127.0.0.1:8080/ 에서 시작합니다. 일반 DB 포트는 15432, 테스트 DB 포트는 15433입니다. 이전 강의의 앱과 컨테이너를 종료한 뒤 실행해 주세요.

Node.js 22가 설치되어 있으면 `npm run test:ui`로 화면 요청 함수 테스트 6개를 실행할 수 있습니다. 별도 npm 패키지 설치는 필요하지 않습니다.

## 코드를 읽는 순서

- `src/main/java/study/shop/order/domain/DiscountPolicy.java`: 상품 합계에서 할인 금액을 얻는 계약
- `src/main/java/study/shop/order/domain/ThresholdFixedDiscountPolicy.java`: 50000원 이상이면 5000원 할인
- `src/main/java/study/shop/order/domain/NoDiscountPolicy.java`: 할인 0원
- `src/main/java/study/shop/order/domain/ShopOrder.java`: 정책 결과 검사와 배송비·최종 금액 계산
- `src/main/java/study/shop/order/service/OrderConfiguration.java`: 기본 정책을 Spring 빈으로 등록
- `src/main/java/study/shop/order/service/OrderService.java`: 정책을 생성자로 받아 주문 확정에 전달
- `src/test/java/study/shop/order/domain/DiscountPolicyTest.java`: 두 정책의 금액과 경계값 8개 검사
- `src/test/java/study/shop/order/service/NoDiscountOrderFlowTest.java`: 테스트 설정에서 할인 없음 정책을 주입한 실제 저장 흐름

## 확인할 결과

| 상품 합계 | 기본 할인 최종 금액 | 할인 없음 최종 금액 | 배송비 |
|---:|---:|---:|---:|
| 49999원 | 52999원 | 52999원 | 3000원 |
| 50000원 | 45000원 | 50000원 | 0원 |
| 60000원 | 55000원 | 60000원 | 0원 |
| 75000원 | 70000원 | 75000원 | 0원 |

배송비는 할인 전 상품 합계로 결정합니다. 기본 앱은 ThresholdFixedDiscountPolicy를 사용합니다. 정책별 테스트는 DB 없이 실행할 수 있습니다.

```bash
./gradlew test --tests study.shop.order.domain.DiscountPolicyTest
```

전체 Java 테스트는 99개, 요청 함수 테스트는 6개입니다. 취소해도 확정 당시 할인과 금액은 유지합니다. 상세 설계는 [정책과 서비스](docs/policy.md), [도메인 지도](docs/domain-map.md), [변경 경계](docs/aggregate.md)를 참고하세요.

로그인 도입 전의 학습용 상점이며 결제·취소는 실제 청구나 환불이 없는 모의 처리입니다.
