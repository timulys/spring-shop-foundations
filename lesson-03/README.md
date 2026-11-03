# 3강 · 상품 조회 책임과 의존성 주입

Controller가 생성자로 ProductService를 받아 상품을 조회합니다.

이 강의 본문의 완성 상태입니다.

## 실행

JDK 21을 설치하고 이 폴더의 build.gradle을 IntelliJ에서 엽니다. Project SDK와 Gradle JVM을 21로 지정한 뒤 ShopApplication을 실행합니다. 터미널에서는 JDK 21 환경에서 ./gradlew bootRun을 실행합니다. Gradle은 별도로 설치하지 않아도 됩니다.

Spring Boot 4.1.2, Gradle Wrapper 8.14.3을 사용합니다. 기본 주소는 http://127.0.0.1:8080 이며 강의 프로젝트는 한 번에 하나만 실행합니다. 코드 변경 후 저장·재실행하고 브라우저를 새로고침합니다. 서버 종료는 IntelliJ의 Stop 또는 터미널의 Ctrl+C입니다.

## 본문 완성 결과

GET /api/products의 상품 순서·이름·가격을 유지합니다. 2강 과제의 category 필드가 반영되어 키보드·마우스는 주변기기, 노트는 문구입니다. 공개 완성 코드는 코드 오름차순입니다.

/hello의 인사·키보드·30000원 응답도 유지합니다. 이 회차의 상품 원천은 자바 코드에 담긴 메모리 목록입니다. 쇼핑몰 과정의 DB 연결은 6강부터 진행합니다.
