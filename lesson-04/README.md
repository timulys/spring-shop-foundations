# 4강 · 상품 코드와 검색 조건 받기

상품 상세와 category·q·maxPrice 조건을 처리합니다.

이 강의 본문의 완성 상태입니다.

## 실행

JDK 21을 설치하고 이 폴더의 build.gradle을 IntelliJ에서 엽니다. Project SDK와 Gradle JVM을 21로 지정한 뒤 ShopApplication을 실행합니다. 터미널에서는 JDK 21 환경에서 ./gradlew bootRun을 실행합니다. Gradle은 별도로 설치하지 않아도 됩니다.

Spring Boot 4.1.2, Gradle Wrapper 8.14.3을 사용합니다. 기본 주소는 http://127.0.0.1:8080 이며 강의 프로젝트는 한 번에 하나만 실행합니다. 코드 변경 후 저장·재실행하고 브라우저를 새로고침합니다. 서버 종료는 IntelliJ의 Stop 또는 터미널의 Ctrl+C입니다.

## 본문 완성 결과

GET /api/products/P001 → 키보드. 없는 P999 상세 → 404. category=문구 → 노트. maxPrice=15000 → 마우스·노트. 여러 조건은 AND이며 경계 가격을 포함합니다. 숫자로 변환하지 못한 가격과 음수는 400, 유효한 빈 목록은 200과 []입니다. q는 String.length() 기준 40까지 받습니다.

/hello의 인사·키보드·30000원 응답도 유지합니다. 이 회차의 상품 원천은 자바 코드에 담긴 메모리 목록입니다. 쇼핑몰 과정의 DB 연결은 6강부터 진행합니다.
