# 33강 · 회원별 장바구니와 주문

로그인한 회원마다 장바구니와 주문을 따로 사용합니다. 다른 회원의 주문 번호를 알아도 조회·취소 API는 404를 반환합니다. 상품 조회는 로그인 없이 이용할 수 있습니다. [소유권과 접근 제어](docs/member-authorization.md)를 함께 읽어 주세요.

## 실행

JDK 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 이전 강의의 앱과 컨테이너를 종료하고 이 폴더에서 실행합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
npm run test:ui
./gradlew bootRun
```

33강은 이 Compose 프로젝트의 새 DB에서 시작합니다. 이전 강의의 DB 볼륨은 보존해 주세요. 기존 공용 주문이나 장바구니 항목이 남은 DB에 V6를 적용하면 소유권 전환을 중단합니다. 이미 회원만 있고 주문·장바구니 항목이 없는 DB에는 회원별 빈 장바구니를 만듭니다.

IntelliJ에서 `build.gradle`을 열고 Project SDK와 Gradle JVM을 21로 지정합니다. Windows에서는 `gradlew.bat`를 사용합니다. 일반 DB는 15432, 테스트 DB는 15433, 앱은 8080 포트를 사용합니다.

1. http://127.0.0.1:8080 에서 로그인 없이 상품을 조회합니다.
2. 회원 A를 가입시키고 로그인합니다. 키보드 2개와 마우스 1개를 담아 모의 결제하면 총액은 70,000원입니다.
3. 별도 브라우저 프로필이나 시크릿 창에서 회원 B를 가입시키고 로그인합니다. B의 장바구니와 주문 내역은 비어 있습니다.
4. B로 A의 주문 상세를 요청하거나 취소하면 404를 받습니다. A의 주문 상태는 유지됩니다.
5. A로 주문을 취소하면 CANCELLED가 됩니다. 다시 취소하면 409입니다.

브라우저 탭 두 개는 같은 쿠키 저장소를 공유할 수 있습니다. 서로 다른 회원을 비교할 때는 쿠키 저장소가 분리된 창을 사용해 주세요. 자동 테스트도 별도 CookieManager를 사용합니다.

Java 테스트 157개, JavaScript 테스트 10개를 제공합니다. 소유권 관련 HTTP 검사는 다음 명령으로 실행합니다.

```bash
./gradlew test --tests 'study.shop.member.security.OwnershipHttpTest'
```

## 핵심 파일

- `cart/controller/CartController.java`, `order/controller/OrderController.java`: 세션의 회원 번호 전달
- `cart/repository/CartRepository.java`, `order/repository/OrderRepository.java`: 회원 조건을 포함한 조회
- `member/service/SignupService.java`: 회원과 빈 장바구니의 원자적 저장
- `member/security/SecurityConfiguration.java`: 장바구니·주문 인증 요구
- `src/main/resources/db/migration/V6__member_ownership.sql`: 외래 키·장바구니 유일 제약·주문 인덱스

Java 파일 경로는 `src/main/java/study/shop/` 기준입니다. 테스트는 전용 `shop_test` DB의 데이터를 초기화합니다. 운영용 DB를 테스트 설정에 연결하지 마세요.
