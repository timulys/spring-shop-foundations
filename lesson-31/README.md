# 31강 · 비밀번호를 해시로 저장하기

회원가입 화면에서 입력을 받아 BCrypt 해시를 만들고 회원을 저장합니다. 같은 아이디로 다시 가입하면 HTTP 409가 반환됩니다. [해시와 가입 처리 설명](docs/password-hashing.md)을 함께 읽어 주세요.

## 실행

JDK 21, Spring Boot 4.1.2, Spring Security Crypto 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 인코더 의존성 버전은 Spring Boot BOM이 관리합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

이전 강의의 앱과 컨테이너를 종료한 뒤 이 강의의 학습 DB를 사용합니다. 일반 DB는 15432, 테스트 DB는 15433 포트입니다. 상점은 http://127.0.0.1:8080/, 회원가입은 http://127.0.0.1:8080/signup.html 에서 엽니다. Windows에서는 `gradlew.bat`를 사용합니다. IntelliJ의 Project SDK와 Gradle JVM은 21로 맞춥니다.

Java 테스트는 139개입니다. 30강의 126개에 비밀번호 정책·해시·가입 HTTP 검사 13개를 추가했습니다. Node.js 22에서 실행하는 JavaScript 테스트는 8개입니다.

```bash
./gradlew test --tests 'study.shop.member.*'
npm run test:ui
```

## 따라 읽을 코드

1. `member/service/PasswordConfiguration.java`: BCrypt 비용 계수 12
2. `member/service/PasswordInputPolicy.java`: 8~64자와 UTF-8 72바이트 제한
3. `member/service/SignupService.java`: 입력 검사 → 해시 계산 → DB 저장
4. `member/controller/MemberController.java`: POST `/api/members`와 HTTP 201
5. `member/dto/SignupRequest.java`, `MemberResponse.java`: 입력·응답 분리
6. `src/main/resources/static/signup.html`, `signup.js`: 가입 화면과 결과 표시
7. `src/test/java/study/shop/member/service`: 정상·경계·동시 가입 결과

위 Java 경로는 `src/main/java/study/shop/` 기준입니다. 30강의 `member/web`에 있던 DTO는 이 강의의 `member/dto`로 옮겼습니다. 새 HTTP 진입점은 `member/controller`, 처리 순서는 `member/service`에 둡니다.

가입 응답은 회원 번호·로그인 아이디·표시 이름만 담습니다. 로그인과 회원별 장바구니·주문 접근은 32~33강에서 연결합니다. 이 체크포인트의 기존 주문·장바구니는 앞 강의의 고정 학습 범위를 유지합니다.
