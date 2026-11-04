# 6강 · Docker로 PostgreSQL 실행하기

IntelliJ에서 이 폴더를 엽니다. Java 21, Docker Desktop과 Compose가 필요합니다. 스프링 코드는 5강 완성본이며, 이번 강의에서는 DB 환경을 추가합니다. 상품 화면은 아직 메모리 목록을 사용합니다.

## 따라 하기

5강 폴더에서 시작한다면 프로젝트 최상위에 `compose.yaml`을 새로 작성합니다. 이 폴더에는 강의의 최종 파일이 들어 있습니다.

```sh
docker version
docker compose version
docker compose config
docker compose up -d --wait
docker compose ps
docker compose exec db psql -U shop -d shop
```

psql에서 다음 명령을 실행합니다.

```sql
SELECT current_database(), current_user;
SHOW server_version;
```

`shop`, `shop`, `18.4`가 표시됩니다. `\q`로 나옵니다.

## 데이터와 컨테이너

```sh
docker compose exec db psql -U shop -d shop -c "CREATE TABLE IF NOT EXISTS lesson_note (id integer PRIMARY KEY, message text NOT NULL); INSERT INTO lesson_note VALUES (1, '다시 만나도 남아 있습니다') ON CONFLICT (id) DO NOTHING;"
docker compose stop db
docker compose start db
docker compose up -d --wait
docker compose exec db psql -U shop -d shop -c "SELECT * FROM lesson_note;"
docker compose down
docker compose up -d --wait
docker compose exec db psql -U shop -d shop -c "SELECT * FROM lesson_note;"
```

`down` 후 컨테이너를 다시 만들어도 이름 있는 볼륨에 저장된 행은 유지됩니다. `down -v`는 해당 프로젝트의 데이터 볼륨까지 삭제하므로 강의의 보존 실습에서는 사용하지 않습니다.

## 호스트에서 연결

- 주소: `127.0.0.1`, 포트: `15432`
- DB: `shop`, 사용자: `shop`, 비밀번호: `shop-local-only`
- 컨테이너 내부 PostgreSQL 포트: `5432`

비밀번호는 localhost에서만 사용하는 공개 실습값입니다. 개인·운영 DB에는 사용하지 않습니다. PostgreSQL 18 공식 이미지의 볼륨 연결 위치는 `/var/lib/postgresql`입니다. 이미지 태그를 다른 주 버전으로 바꿔 기존 볼륨을 그대로 연결하지 마세요.

포트 변경 과제는 기존 컨테이너를 `docker compose down`으로 종료한 뒤 실행합니다.

```sh
POSTGRES_PORT=15433 docker compose up -d --wait
```

호스트 접속 포트도 `15433`으로 바꿉니다. 컨테이너 내부 `exec` 접속에는 호스트 포트가 사용되지 않습니다. 기본 포트로 돌아갈 때는 `docker compose down` 후 `docker compose up -d --wait`를 실행합니다.

스프링 앱은 `ShopApplication`을 실행하거나 `./gradlew bootRun`으로 시작합니다. 화면 주소는 `http://localhost:8080/`입니다. 이 강의에서는 앱과 DB를 아직 연결하지 않습니다.
