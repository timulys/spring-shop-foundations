# 38강 · 상품과 주문 서비스를 나누는 기준

현재 상점의 호출 관계와 데이터 책임을 살펴보고, 상품 조회를 별도 서비스로 옮길 계획을 세웁니다. 이 체크포인트의 실행 코드는 37강과 같은 단일 Spring 애플리케이션입니다. 39강에서 상품 앱과 DB를 추가하고, 40강에서 주문의 상품 조회를 HTTP 호출로 전환합니다.

- [서비스 경계와 분리 순서](docs/service-boundaries.md)
- [상품 묶음 조회 계약](docs/catalog-lookup-contract.md)
- [요청·응답 예시](contracts/catalog-lookup-examples.json)

## 실행

이전 강의의 앱과 컨테이너를 종료한 뒤 다음 명령을 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build --wait
```

Windows에서는 파일 탐색기나 `Copy-Item .env.example .env`로 복사할 수 있습니다. http://127.0.0.1:8080 에서 가입·로그인한 뒤 키보드 2개와 마우스 1개를 주문합니다. 총액은 70,000원입니다. 주문에 저장된 상품명·단가와 현재 상품 테이블의 역할을 비교해 주세요.

## 코드 검사와 환경

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
npm run test:ui
```

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 호스트 검사에는 JDK 21과 Node.js가 필요합니다. IntelliJ의 Project SDK와 Gradle JVM을 21로 지정합니다. Windows에서는 `gradlew.bat`를 사용합니다. Java 검사 160개와 JavaScript 검사 10개를 제공합니다.

실습이 끝나면 `docker compose --profile test down`으로 컨테이너를 종료합니다. DB 볼륨은 유지됩니다. 내부 상태 검사는 [37강의 상태 검사 설명](docs/container-health.md)을 참고해 주세요.
