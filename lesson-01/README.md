# 1강 · 첫 스프링 부트 서버

Java 21 · Spring Boot 4.1.2 · Gradle Wrapper 8.14.3

## 실행

JDK 21을 설치하고 IntelliJ에서 `build.gradle`이 들어 있는 프로젝트 폴더를 엽니다. Project SDK와 Gradle JVM을 21로 지정합니다. Gradle 불러오기가 끝난 뒤 `study.shop.ShopApplication`의 main을 실행합니다.

브라우저: http://127.0.0.1:8080/hello

```text
안녕하세요. 쇼핑몰 첫 서버입니다.
상품: 키보드
가격: 30000원
```

터미널에서는 JDK 21을 선택한 상태에서 실행합니다. Gradle을 별도로 설치할 필요는 없습니다.

```sh
./gradlew bootRun
```

기본 주소는 내 컴퓨터에서만 접속하는 `127.0.0.1:8080`입니다. 종료는 IntelliJ의 Stop 또는 터미널의 Ctrl+C입니다. 중지한 뒤 브라우저를 새로고침하면 연결이 거부됩니다. 다른 프로그램이 8080을 사용하면 먼저 해당 프로그램과의 충돌을 확인하세요.

문구를 수정했다면 저장 후 재실행하고 브라우저를 새로고침합니다. 이 프로젝트에는 자동 재시작 도구가 없습니다.
