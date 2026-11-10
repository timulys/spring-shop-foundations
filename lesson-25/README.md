# 25강 · Money 값 객체

장바구니와 주문의 금액 계산을 불변 값 객체 Money로 모읍니다. 같은 금액의 값 비교, 음수 거부, 계산 범위 초과를 확인합니다. API의 숫자 형식과 데이터베이스 테이블은 유지합니다.

## 실행

JDK 21과 Docker가 필요합니다. Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. http://127.0.0.1:8080/ 에서 시작합니다. IntelliJ에서 build.gradle을 열고 Project SDK와 Gradle JVM을 21로 지정합니다. 일반 DB 포트는 15432, 테스트 DB 포트는 15433입니다. 이전 강의의 앱과 컨테이너를 종료한 뒤 실행해 주세요.

공통 요청 함수의 테스트는 Node.js 22 환경에서 `npm run test:ui`로 실행합니다. 설치할 npm 패키지는 없습니다. 브라우저와 서버 실행에는 Node.js가 필요하지 않습니다.

## 패키지 기준

```text
study.shop
├── ShopApplication
├── catalog   # 상품 조회와 현재 판매 정보
├── cart      # 담은 상품과 수량 변경
├── order     # 주문 당시 가격, 금액, 상태 전이
└── common
    └── money # Money 값 객체와 금액 연산
```

각 기능의 Controller, Service, Repository, 응답 객체를 같은 기능 패키지에 둡니다. `ShopApplication`은 루트에 두어 하위 패키지를 탐색하게 합니다. Money 테스트는 common.money에, 주문 금액 테스트는 order에 둡니다.

- 상품의 현재 가격은 `catalog.Product`, 주문 당시 단가는 `order.OrderItem`이 보관합니다.
- 주문 확정과 취소 상태 규칙은 `order.ShopOrder`에 있습니다.
- `OrderItem.lineAmount`와 `ShopOrder.confirm`, `CartService.snapshot`에서 Money를 사용합니다.
- 주문 항목의 1~99 수량 검증은 주문 항목 생성자에서 지킵니다. 장바구니 내부 메서드를 외부에 공개하지 않습니다.

이 단계에서는 주문 코드가 상품과 장바구니 코드를 직접 참조합니다. 후속 강의에서 할인·배송비 정책과 상품 조회 계약을 정리합니다. 판단 기준과 현재 의존 관계는 [도메인 지도](docs/domain-map.md)에 있습니다.

## 실행 결과 확인

1. 키보드 2개와 마우스 1개를 담습니다. 상품 합계는 75000원입니다.
2. 주문 내용을 확인합니다. 할인 5000원, 배송비 0원, 최종 금액 70000원이 표시됩니다.
3. 모의 결제 후 주문 상세를 조회하고 취소합니다. 새로고침 후에도 취소 상태와 주문 금액 70000원이 유지됩니다.
4. 전체 Java 테스트 82가지와 요청 함수 테스트 6가지를 실행합니다. 수량 0과 100을 거부하는 주문 경계 검사도 포함합니다.

이 프로젝트는 로그인 전 학습용 상점입니다. 결제와 취소는 실제 청구나 환불이 없는 모의 처리입니다.

## 이번 강의의 금액 계산

`common/money/Money.java`에서 값 비교와 불변 계산, 음수·오버플로 검증을 확인하세요. 장바구니와 주문은 Money로 계산한 뒤 저장·응답 경계에서 long으로 변환합니다. 자세한 기준은 [Money 설명](docs/money.md)에 있습니다.
