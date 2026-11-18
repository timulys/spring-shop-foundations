# 36강 · 설정과 데이터를 보존하며 변경하기

Compose 설정은 환경변수로 전달하고, DB 변경은 Flyway V7로 적용합니다. 새로 취소한 주문에는 취소 시각을 저장합니다. 이전 버전에서 이미 취소한 주문은 상태와 금액을 유지하며 취소 시각은 NULL로 남습니다. [설정·볼륨·마이그레이션 설명](docs/config-migrations.md)을 함께 읽어 주세요.

## 새 DB에서 실행

이전 강의 컨테이너를 종료한 뒤 이 폴더에서 실행합니다.

```bash
cp .env.example .env
docker compose up -d --build
docker compose logs -f app
```

Windows에서는 파일 탐색기나 `Copy-Item .env.example .env`로 복사할 수 있습니다. 제공한 값은 로컬 학습용입니다. `.env`는 Git과 이미지 빌드에서 제외되고 `.env.example`만 공유합니다. `DB_PASSWORD`가 비어 있으면 Compose 실행이 중단됩니다.

앱 로그에서 `Started ShopApplication`을 확인하고 http://127.0.0.1:8080 에 접속합니다. 회원가입·로그인 후 키보드 2개와 마우스 1개를 주문하면 총액은 70,000원입니다. 주문을 취소하면 상세 화면에 취소 시각이 표시됩니다.

Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, PostgreSQL 18.4, Gradle Wrapper 8.14.3을 사용합니다. 호스트에서 Java 158개·JavaScript 10개 테스트를 실행할 수 있습니다.

```bash
docker compose --profile test up -d --wait test-db
./gradlew test
npm run test:ui
```

테스트는 `shop_test` 전용 DB와 고정된 테스트용 비밀번호를 사용합니다. IDE에서 일반 앱을 실행할 때는 35강처럼 `compose.dev.yaml`로 DB 포트를 연결합니다. `.env`의 DB 비밀번호를 바꿨다면 IntelliJ 실행 환경의 `DB_PASSWORD`에도 같은 값을 넣습니다. 호스트 포트를 바꾼 경우 `DB_URL` 또는 `TEST_DB_URL`도 해당 포트에 맞춥니다.

## 35강 데이터가 있는 DB 갱신

전체 코드 ZIP을 풀면 `lesson-35`와 `lesson-36`이 나란히 있습니다. 아래 명령은 `lesson-36` 폴더에서 실행합니다. 이 비교 실습에서는 예시 DB 비밀번호 `shop-local-only`를 유지합니다. 다른 앱이 8080을 쓰고 있다면 먼저 종료합니다.

```bash
docker compose -p shop-migration-demo -f ../lesson-35/compose.yaml up -d --build
```

35강 화면에서 회원을 가입시키고 주문 두 개를 만든 뒤 한 개를 취소합니다. 다음 명령으로 35강 컨테이너를 종료하고 같은 프로젝트 이름으로 36강을 시작합니다.

```bash
docker compose -p shop-migration-demo -f ../lesson-35/compose.yaml down
docker compose -p shop-migration-demo up -d --build
```

같은 프로젝트 이름과 볼륨 키를 사용하므로 기존 DB 볼륨에 다시 연결됩니다. 다시 로그인하면 두 주문의 번호·상품·금액·상태가 유지됩니다. 이전에 취소한 주문에는 취소 시각 기록이 없고, 남은 확정 주문을 지금 취소하면 시각이 기록됩니다.

```bash
docker compose -p shop-migration-demo exec db psql -U shop -d shop -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank"
docker compose -p shop-migration-demo up -d --force-recreate
```

재생성 뒤 다시 로그인해 주문을 확인합니다. V7 적용 이력은 한 행입니다. `down`은 DB 볼륨을 남깁니다. `down -v`는 이 실습의 저장 데이터를 지우므로 데이터 보존 실습에서는 사용하지 않습니다.
