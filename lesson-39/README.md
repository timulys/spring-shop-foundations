# 39강 · 상품 서비스를 별도 애플리케이션으로 실행하기

상품 앱과 상품 DB를 독립적으로 실행합니다. `catalog-service`는 현재 상품 목록과 내부 묶음 조회 API를 제공하고 `catalog-db`만 사용합니다. `order-service`는 회원·장바구니·주문을 실행합니다. 이번 강의에서는 기존 상점의 로컬 상품 조회를 유지하며, 40강에서 그 조회를 HTTP 호출로 바꿉니다.

| 폴더 | 열고 실행할 프로젝트 |
|---|---|
| `catalog-service` | 독립 상품 Spring 앱 |
| `order-service` | 기존 상점에서 이어 온 주문 Spring 앱 |
| 루트의 `compose.yaml` | 두 앱·두 DB·NGINX를 함께 실행하는 구성 |

## 전체 실행

이전 강의의 상점 컨테이너와 IDE 실행을 종료한 뒤 루트에서 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build --wait
docker compose ps
```

Windows에서는 `Copy-Item .env.example .env`로 복사할 수 있습니다. 상점 화면은 http://127.0.0.1:8080 이고, 독립 상품 API는 http://127.0.0.1:18081/api/products 입니다. 상품 API의 18081 포트는 이번 독립 실행 실습을 위해 호스트의 루프백 주소에 연결했습니다.

상품 앱의 `/api/products`에는 P001 키보드 30,000원, P002 마우스 15,000원, P003 노트 5,000원이 나옵니다. 상점에 가입·로그인한 뒤 키보드 2개와 마우스 1개를 주문하면 총액은 70,000원입니다.

```bash
curl -H 'Content-Type: application/json' \
  -d '{"productCodes":["P002","P001"]}' \
  http://127.0.0.1:18081/internal/products/lookup
```

응답에는 코드순으로 P001·P002의 코드·이름·단가가 들어갑니다. PowerShell에서는 다음과 같이 호출할 수 있습니다.

```powershell
Invoke-RestMethod -Method Post -ContentType 'application/json' -Body '{"productCodes":["P002","P001"]}' -Uri 'http://127.0.0.1:18081/internal/products/lookup'
```

## 독립 빌드와 검사

Java 21, Spring Boot 4.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 주문 앱의 Spring Security는 7.1.2입니다. 호스트 검사에는 JDK 21과 Node.js가 필요합니다. IntelliJ에서는 각 서비스 폴더를 별도 Gradle 프로젝트로 열고 Project SDK와 Gradle JVM을 21로 지정합니다.

루트에서 테스트 DB를 시작합니다.

```bash
docker compose --profile test up -d --wait order-test-db catalog-test-db
```

`catalog-service` 폴더에서는 `./gradlew test bootJar`를 실행합니다. 상품 앱의 검사 23개가 상품 검색·묶음 조회·누락·중복·길이·자료형·저장소 동작을 다룹니다. `order-service` 폴더에서는 `./gradlew test bootJar`와 `npm run test:ui`를 실행합니다. 주문 앱의 Java 검사 160개, JavaScript 검사 10개가 있습니다. Windows에서는 `gradlew.bat`를 사용합니다.

## 상품 앱만 실행하기

컨테이너만 사용할 때는 루트에서 다음 명령으로 상품 앱과 자기 DB만 시작할 수 있습니다.

```bash
docker compose up -d --build --wait catalog
```

IntelliJ에서 실행할 때는 먼저 앱 컨테이너를 종료하고 호스트용 DB 포트를 연결합니다.

```bash
docker compose stop catalog
docker compose -f compose.yaml -f compose.dev.yaml up -d --wait catalog-db
```

`catalog-service`에서 `CatalogApplication`을 실행하면 http://127.0.0.1:8081/api/products 에 접속할 수 있습니다. 기본 접속 값은 DB `catalog`, 사용자 `catalog`, 비밀번호 `catalog-local-only`, DB 포트 15435입니다. `.env`의 값을 바꿨다면 IDE의 `DB_URL`, `DB_USER`, `DB_PASSWORD`도 같은 값으로 지정합니다. Compose가 읽는 `.env`는 IDE에 자동 적용되지 않습니다.

실습 후 루트에서 `docker compose --profile test down`을 실행합니다. 호스트용 DB 설정으로 실행했다면 같은 `-f compose.yaml -f compose.dev.yaml` 옵션을 사용합니다. DB 볼륨은 유지됩니다.

- [데이터 소유권과 재시작 실험](docs/independent-catalog.md)
- [내부 상품 조회 계약](docs/catalog-lookup-contract.md)
- [분리 계획](docs/service-boundaries.md)
