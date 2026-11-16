# 26강 · 주문 애그리거트와 변경 경계

주문은 `ShopOrder`를 통해 항목 추가·확정·취소를 처리합니다. 상품·장바구니·주문의 내부 패키지를 역할에 맞춰 나누고, 외부에서 주문 목록과 상태 규칙을 우회할 수 없도록 검사합니다.

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

- `src/main/java/study/shop/order/domain/ShopOrder.java`: draft·addItem·confirm·cancel과 목록 반환
- `src/main/java/study/shop/order/domain/OrderItem.java`: 주문 당시 단가·수량, 외부 변경 메서드가 없는 항목
- `src/main/java/study/shop/order/service/OrderService.java`: 조회·저장·장바구니 비우기를 묶는 트랜잭션
- `src/main/java/study/shop/order/repository/OrderRepository.java`: DTO 전체 경로를 쓰는 JPQL과 상태 변경 잠금
- `src/test/java/study/shop/order/domain/OrderAggregateTest.java`: DB 없이 실행하는 변경 경계 테스트 7개

상품·장바구니·주문 각각의 내부에 controller·service·domain·repository·dto를 둡니다. `ShopApplication`은 study.shop에 두어 하위 코드를 탐색합니다. 공통 오류 처리와 금액은 common에 있습니다. 자세한 기준은 [도메인 지도](docs/domain-map.md)와 [주문 변경 규칙](docs/aggregate.md)을 참고하세요.

## 확인할 결과

키보드 2개와 마우스 1개는 상품 합계 75,000원, 할인 5,000원, 배송비 0원, 최종 70,000원입니다. 취소 뒤에도 항목과 원래 금액은 남습니다. 확정 후 항목 추가, 취소 후 재확정, 반복 취소는 거부합니다. 반환한 항목 목록도 추가·삭제할 수 없습니다.

전체 Java 테스트는 89개입니다. 도메인 검사만 실행하려면 다음 명령을 사용합니다.

```bash
./gradlew test --tests study.shop.order.domain.OrderAggregateTest
```

이 프로젝트는 로그인 도입 전의 학습용 상점입니다. 결제와 취소는 실제 청구나 환불이 없는 모의 처리입니다.
