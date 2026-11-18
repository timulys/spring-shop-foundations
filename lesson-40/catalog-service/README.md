# 상품 서비스

이 폴더를 IntelliJ의 독립 Gradle 프로젝트로 엽니다. Java와 Gradle JVM은 21을 사용합니다. `CatalogApplication`은 기본 8081 포트에서 실행하며 자기 DB인 `catalog`에 접속합니다.

DB 시작·환경변수·테스트 DB 실행 명령은 [전체 실행 안내](../README.md)에 있습니다. 테스트 DB가 준비되면 이 폴더에서 `./gradlew test bootJar`를 실행합니다. Windows에서는 `gradlew.bat`를 사용합니다.

외부 상품 조회와 내부 묶음 조회만 담당하며, 회원·장바구니·주문 테이블을 만들지 않습니다. Docker 이미지에서는 `SERVER_PORT=8080`을 사용하고 Compose가 호스트 18081 포트에 연결합니다.
