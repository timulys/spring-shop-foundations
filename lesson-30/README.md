# 30강 · 회원 모델과 가입 입력 만들기

회원 번호와 로그인 아이디를 구분하고, 정규화한 아이디의 중복을 DB에서 막습니다. [회원 모델 설명](docs/member-model.md)을 함께 읽어 주세요.

## 실행

JDK 21, Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다.

```bash
docker compose --profile test up -d --wait db test-db
./gradlew test
./gradlew bootRun
```

이전 강의의 앱과 컨테이너를 종료한 뒤 이 강의의 새 학습 DB를 사용합니다. 일반 DB는 15432, 테스트 DB는 15433, 상점은 http://127.0.0.1:8080/ 입니다. Windows에서는 `gradlew.bat`를 사용합니다. IntelliJ의 Project SDK와 Gradle JVM은 21로 맞춥니다.

전체 Java 테스트 126개와 Node.js 22의 `npm run test:ui` 테스트 6개가 통과합니다. 회원 테스트 15개는 정규화·입력 경계·해시 제외·서로 다른 회원 번호·DB 중복 제약을 확인합니다.

```bash
./gradlew test --tests 'study.shop.member.*'
npm run test:ui
```

## 코드 순서

1. `member/domain/LoginIds.java`: 로그인 아이디 정규화
2. `member/domain/Member.java`: DB 번호와 로그인 아이디, 해시 저장 모델
3. `db/migration/V5__create_members.sql`: NOT NULL·UNIQUE·CHECK
4. `member/web/SignupRequest.java`와 `MemberResponse.java`: 입력과 응답 분리
5. `member/repository/MemberRepository.java`: 정규화된 아이디 조회
6. `src/test/java/study/shop/member`: 두 회원 저장과 중복 거부 테스트

기본 상점의 주문·취소 결과는 29강과 같습니다. 회원 테스트에서는 공개된 고정 해시 예제만 사용합니다. 실제 해시 생성과 가입 API·화면은 31강에서 연결하며, 이 강의에는 가입 엔드포인트가 없습니다.
