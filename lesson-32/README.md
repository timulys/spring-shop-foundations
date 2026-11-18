# 32강 · 세션 로그인과 로그아웃

회원가입 후 로그인하면 세션 쿠키로 인증 상태가 유지됩니다. 로그아웃 뒤 내 정보 API는 HTTP 401을 반환합니다. [세션과 CSRF 설명](docs/session-login.md)을 함께 읽어 주세요.

## 실행

JDK 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

IntelliJ에서는 이 폴더의 `build.gradle`을 열고 Project SDK와 Gradle JVM을 21로 맞춥니다. Windows에서는 `gradlew.bat`를 사용합니다. 이전 강의의 앱과 DB를 종료한 뒤 실행합니다. 일반 DB 포트는 15432, 테스트 DB 포트는 15433입니다.

1. http://127.0.0.1:8080/signup.html 에서 새 회원을 만듭니다.
2. http://127.0.0.1:8080/login.html 에서 로그인합니다.
3. 화면을 새로고침해 회원 정보가 유지되는지 확인합니다.
4. 로그아웃한 뒤 내 정보 API의 401 응답을 확인합니다.

전체 Java 테스트는 148개, JavaScript 테스트는 10개입니다.

```bash
./gradlew test --tests 'study.shop.member.security.SessionHttpTest'
npm run test:ui
```

## 주요 파일

- `member/security/SecurityConfiguration.java`: 인증할 경로·formLogin·logout·실패 응답
- `member/security/MemberDetailsService.java`: 정규화한 아이디로 회원을 찾는 UserDetailsService
- `member/security/ShopPrincipal.java`: 세션에 사용할 회원 정보와 자격 증명 제거
- `member/controller/SessionController.java`: 내 정보와 CSRF 토큰 API
- `static/api.js`: 변경 요청 전에 토큰을 받아 헤더로 전달
- `static/login.html`, `login.js`: 로그인·새로고침·로그아웃 화면

Java 파일은 `src/main/java/study/shop/`, 화면 파일은 `src/main/resources/` 기준입니다. 로그인 입력 필드는 Spring Security formLogin의 기본 이름인 `username`, `password`를 사용합니다. 성공과 로그아웃은 HTTP 204, 잘못된 로그인은 HTTP 401입니다.

이 강의에서 인증을 요구하는 경로는 `/api/session/**`입니다. 장바구니와 주문은 기존 학습 범위를 유지하며, 회원별 소유권과 접근 제한은 33강에서 연결합니다. CSRF 검사는 이 단계부터 상태를 바꾸는 요청 전체에 적용합니다.
