# 7강 · 상품 테이블과 SQL

6강에서 만든 DB 실행 환경에 상품 테이블과 세 상품을 추가합니다. 상품 API와 웹 화면은 아직 자바의 메모리 목록을 읽습니다. DB 연결은 8강, 상품 조회 교체는 9강에서 이어집니다.

## 실행

Java 21, Docker Desktop, Gradle Wrapper를 사용합니다. PostgreSQL은 `18.4`, Spring Boot는 `4.1.2`입니다.

다른 강의의 DB가 `15432` 포트를 사용하고 있다면 해당 강의 폴더에서 `docker compose stop db`를 먼저 실행해 주세요. 강의별 Compose 이름과 볼륨이 달라 데이터가 섞이지 않습니다.

```bash
docker compose up -d --wait
docker compose exec -T db psql -U shop -d shop -v ON_ERROR_STOP=1 < db/01-schema.sql
docker compose exec -T db psql -U shop -d shop -v ON_ERROR_STOP=1 < db/02-sample-data.sql
docker compose exec db psql -U shop -d shop
```

마지막 명령으로 들어간 `psql`에서 `db/03-practice.sql`의 문장을 하나씩 입력합니다. 중복 코드·0원·이름 NULL 삽입은 실패해야 합니다. 이 파일에는 의도한 실패가 있으므로 전체를 한꺼번에 실행하지 마세요.

| 코드 | 이름 | 카테고리 | 원 단가 |
|---|---|---|---:|
| P001 | 키보드 | 주변기기 | 30000 |
| P002 | 마우스 | 주변기기 | 15000 |
| P003 | 노트 | 문구 | 5000 |

P001을 32000원으로 바꾼 뒤 확인하고 30000원으로 되돌립니다. `WHERE` 없이 UPDATE하면 모든 행이 대상이 됩니다. `NOT NULL`은 NULL을 막으며 빈 문자열까지 막는 규칙은 아닙니다. 기본 키는 코드 중복과 NULL을 막습니다. 이름은 기본 키가 아니므로 같은 이름의 다른 코드가 존재할 수 있습니다.

초기화 SQL을 다시 실행해도 이미 있는 상품을 덮어쓰지 않습니다. `IF NOT EXISTS`는 기존 테이블 구조를 고치거나 검증하지 않습니다. 테이블 정의를 바꿨다면 기존 구조를 별도로 확인해야 합니다. 8강에서는 변경 파일의 적용 기록을 관리하는 Flyway를 사용합니다.

`\q`로 psql에서 나온 뒤 `./gradlew bootRun`으로 기존 화면을 실행할 수 있습니다. 주소는 <http://localhost:8080/>입니다.

## 직접 해 볼 일

가격이 0인 상품을 넣으면 어떤 제약 조건 이름이 나오는지 확인하고, `SELECT count(*) FROM products;`가 3인지 확인해 주세요. 이어서 NULL 이름의 실패 이유를 비교합니다.

## 종료

```bash
docker compose down
```

이름 있는 볼륨은 유지됩니다. `down -v`는 실습 데이터를 지우므로 데이터 보존을 확인하는 이 과정에서는 사용하지 않습니다.
