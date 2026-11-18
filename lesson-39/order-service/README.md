# 주문 서비스 · HTTP 전환 전 단계

이 폴더를 IntelliJ의 독립 Gradle 프로젝트로 엽니다. Java와 Gradle JVM은 21을 사용합니다. `ShopApplication`이 회원·장바구니·주문 기능을 실행합니다.

이번 39강은 독립 상품 앱을 추가하는 단계입니다. 현재 주문 앱은 자기 DB에 남아 있는 상품 테이블을 조회하며, 40강에서 `ProductLookup`의 HTTP 구현으로 전환합니다. 현재 NGINX의 상품 경로도 주문 앱으로 연결되어 있습니다.

DB 시작과 전체 컨테이너 실행은 [루트 안내](../README.md)를 사용합니다. 테스트 DB가 준비되면 이 폴더에서 `./gradlew test bootJar`와 `npm run test:ui`를 실행합니다. Windows에서는 `gradlew.bat`를 사용합니다.
