# 40강 · 상품 조회를 HTTP 호출로 바꾸기

주문 앱이 `ProductLookup`의 HTTP 구현으로 현재 상품 값을 가져옵니다. 주문 DB의 상품 테이블은 V8 마이그레이션에서 제거합니다. 상품명·가격의 원본은 `catalog-db`에 있고, 확정된 주문은 자기 DB에 저장한 당시 값을 사용합니다.

## 실행

이전 강의의 앱과 컨테이너를 종료한 뒤 이 폴더에서 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build --wait
```

Windows에서는 `Copy-Item .env.example .env`로 복사할 수 있습니다. http://127.0.0.1:8080 에서 가입·로그인하고 키보드 2개와 마우스 1개를 주문합니다. 상품 합계 75,000원, 할인 5,000원, 배송비 0원으로 총액은 70,000원입니다.

독립 상품 API는 http://127.0.0.1:18081/api/products 입니다. 이번 강의의 상점 상품 목록은 기존 URL을 유지하기 위해 주문 앱의 임시 전달 코드를 거쳐 상품 앱에 요청합니다. 장바구니와 주문 계산은 내부 묶음 조회를 사용합니다. 41강에서는 브라우저의 상품 경로를 NGINX에서 상품 앱으로 직접 연결합니다.

## 검사

루트에서 두 테스트 DB를 시작합니다.

```bash
docker compose --profile test up -d --wait order-test-db catalog-test-db
```

- `catalog-service` 폴더: `./gradlew test bootJar` — Java 검사 23개.
- `order-service` 폴더: `./gradlew test bootJar` — Java 검사 190개.
- `order-service` 폴더: `npm run test:ui` — JavaScript 검사 10개.

Windows에서는 `gradlew.bat`를 사용합니다. 주문 앱 테스트는 테스트 코드에 포함된 로컬 HTTP 서버를 상품 서비스로 사용합니다. 이 서버로 누락·오류 응답·가격 변경·응답 지연을 재현합니다. 실행용 JAR에는 이 테스트 서버가 포함되지 않습니다.

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, Spring Framework 7.0.10, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. VS Code에서 루트 폴더를 열고 Extension Pack for Java를 설치한 뒤 Java 실행 환경과 Gradle JVM을 21로 지정합니다. 각 서비스의 테스트 파일에서 `Test: Run Tests in Current File` 명령을 실행할 수 있습니다. 주문 테스트 DB의 기본 호스트 포트는 15433, 상품 테스트 DB는 15434입니다. 포트를 바꿨다면 VS Code의 `java.test.config.env`에도 해당 서비스의 `TEST_DB_URL`을 지정합니다. `.env`의 값은 Java 테스트 실행기에 자동으로 전달되지 않습니다. Docker 빌드 파일에는 Gradle 다운로드 캐시를 공유하는 준비 설정이 들어 있습니다. 동시 빌드가 같은 캐시를 수정하지 않도록 `sharing=locked`를 사용합니다.

## IDE에서 주문 앱 실행하기

루트에서 화면·주문 컨테이너를 중단하고, 상품 앱과 주문 DB를 시작합니다.

```bash
docker compose stop web order
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait order-db catalog
```

`order-service`에서 `ShopApplication`을 실행합니다. 기본 주문 DB 주소는 `jdbc:postgresql://127.0.0.1:15432/shop`, 사용자 `shop`, 비밀번호 `shop-local-only`입니다. 기본 상품 주소는 `http://127.0.0.1:18081`입니다. 값을 바꿨다면 IDE의 `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CATALOG_BASE_URL`에 같은 값을 지정합니다. 호스트에서 실행하는 주문 앱의 화면은 http://127.0.0.1:8080 입니다.

실습 후 루트에서 `docker compose --profile test down`으로 종료합니다. 호스트용 DB 포트 설정으로 실행했다면 같은 `-f compose.yaml -f compose.dev.yaml` 옵션을 사용합니다. DB 볼륨은 유지됩니다.

- [원격 조회와 짧은 쓰기 트랜잭션](docs/http-product-lookup.md)
- [상품 조회 계약](docs/catalog-lookup-contract.md)
- [39강 데이터로 전환하기](docs/upgrade-from-39.md)
