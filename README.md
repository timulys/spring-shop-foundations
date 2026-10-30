# 스프링 부트로 쇼핑몰 만들기

자바 기초 과정의 상품·주문 예제를 Spring Boot·JPA·DDD·Docker·MSA로 확장하는 강의 실습 코드입니다. 제작을 마친 회차의 최종 프로젝트만 제공합니다.

| 강의 | 완성 코드 | 확인할 결과 |
|---|---|---|
| 1강 · 첫 서버 실행 | [lesson-01](lesson-01) | `/hello`에 인사·키보드·30000원 표시 |

[전체 코드 ZIP 다운로드](https://github.com/timulys/spring-shop-foundations/archive/refs/heads/main.zip)

JDK 21을 설치하고 강의 폴더의 `build.gradle`을 IntelliJ에서 엽니다. Project SDK와 Gradle JVM을 21로 지정한 뒤 `ShopApplication`을 실행합니다. 터미널에서는 강의 폴더에서 `./gradlew bootRun`을 실행합니다.

검증 환경은 Spring Boot 4.1.2, Gradle Wrapper 8.14.3, Java 21(Zulu 21.0.7)입니다. Gradle을 별도로 설치할 필요는 없습니다. 처음 실행할 때 라이브러리 다운로드에 인터넷이 필요합니다.

브라우저에서 http://127.0.0.1:8080/hello 에 접속합니다. 현재 예제는 내 컴퓨터에서만 접속하며 DB를 사용하지 않습니다. 변경 후에는 저장·재실행·새로고침을 해 주세요. 서버 중지는 IntelliJ의 Stop 또는 Ctrl+C입니다.
