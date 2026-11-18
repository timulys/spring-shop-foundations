# 상품 서비스

현재 상품 목록·검색·단일 조회와 내부 묶음 조회를 제공합니다. 자기 DB의 상품 테이블만 사용하며 회원·세션·주문 데이터는 저장하지 않습니다.

이 폴더를 독립 Gradle 프로젝트로 열고 Java와 Gradle JVM을 21로 지정합니다. 테스트 DB가 준비되면 `./gradlew test bootJar`를 실행합니다. Windows에서는 `gradlew.bat`를 사용합니다.

기본 Compose 구성에서는 상품 앱의 포트를 호스트에 연결하지 않습니다. 브라우저는 NGINX의 `/api/products` 경로로 접속합니다. `compose.dev.yaml`을 함께 적용하면 개발 실습용 18081 포트를 사용할 수 있습니다. 상세 명령은 [루트 안내](../README.md)에 있습니다.
