# 스프링 부트로 쇼핑몰 만들기

자바 기초 과정의 상품·주문 예제를 Spring Boot·JPA·DDD·Docker·MSA로 확장하는 강의 실습 코드입니다. 강의별 본문 완성 프로젝트 하나를 제공합니다.

| 강의 | 완성 코드 | 확인할 결과 |
|---|---|---|
| 1강 · 첫 서버 실행 | [lesson-01](lesson-01) | /hello 인사·키보드·30000원 |
| 2강 · JSON 상품 목록 | [lesson-02](lesson-02) | 상품 세 개의 코드·이름·숫자 가격 |
| 3강 · 서비스와 생성자 주입 | [lesson-03](lesson-03) | 조회 분리, category 포함 목록 유지 |
| 4강 · 상세와 검색 조건 | [lesson-04](lesson-04) | 상세·필터, 200·400·404 구분 |
| 5강 · 첫 웹 화면 | [lesson-05](lesson-05) | API의 실제 상품명·가격을 카드로 표시 |

[전체 코드 ZIP 다운로드](https://github.com/timulys/spring-shop-foundations/archive/refs/heads/main.zip)

JDK 21을 설치하고 원하는 강의 폴더의 build.gradle을 IntelliJ에서 엽니다. Project SDK와 Gradle JVM을 21로 지정한 뒤 ShopApplication을 실행합니다. 터미널 실행은 강의 폴더에서 ./gradlew bootRun입니다. Gradle을 별도 설치할 필요는 없습니다.

기준 환경은 Spring Boot 4.1.2, Gradle Wrapper 8.14.3, Java 21(Zulu 21.0.7)입니다. 서버는 127.0.0.1:8080을 사용하므로 한 강의만 실행합니다. 1강은 /hello, 2~4강은 /api/products, 5강은 / 주소에서 결과를 확인합니다.

1~5강의 상품은 메모리 목록이며 DB 연결은 6강부터 진행합니다. 2강 본문은 세 필드이고, 3강부터 2강 카테고리 과제 정답을 반영합니다. 자바 코드 수정 후 저장·재실행합니다. 자세한 조건은 각 강의 README에 있습니다.
