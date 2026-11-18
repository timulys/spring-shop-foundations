# 설정 변경과 DB 변경을 적용하는 순서

같은 소스와 이미지에 실행 환경변수를 전달하면 접속 설정을 바꿀 수 있습니다. 데이터 구조를 바꿀 때는 새 Flyway 변경 파일을 추가합니다. 컨테이너를 다시 만들더라도 같은 DB 볼륨에 연결하면 기존 회원과 주문을 계속 사용합니다.

## Compose가 읽는 값과 앱이 받는 값

Compose는 `.env`나 셸 환경의 값을 읽어 `${DB_PASSWORD}` 같은 자리를 채웁니다. `environment`에 지정한 값은 해당 컨테이너의 환경변수로 전달됩니다. 이 예제에서 `.env` 파일을 읽는 도구는 Compose입니다. 호스트의 IntelliJ에서 실행할 때는 실행 설정의 환경변수에 필요한 값을 지정합니다. [Compose 환경변수 치환](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)

`DB_PASSWORD`는 앱의 접속 비밀번호와 PostgreSQL 초기 비밀번호에 같은 값으로 전달합니다. `POSTGRES_PASSWORD`는 비어 있는 데이터 디렉터리에 DB를 처음 만들 때 적용됩니다. 기존 DB의 사용자 비밀번호를 바꾸려면 DB 사용자 변경 절차도 함께 필요합니다. 이 강의의 기존 DB 갱신 실습은 비밀번호를 유지합니다. [PostgreSQL 공식 이미지](https://hub.docker.com/_/postgres)

## V7이 추가하는 값

`V7__order_cancellation_time.sql`은 `orders.cancelled_at` 열을 추가합니다. 확정 주문은 NULL이고, 새 취소가 성공하면 앱이 취소 시각을 기록합니다. 취소 요청이 반복되어 409로 끝나면 처음 저장한 시각이 유지됩니다. DB 제약도 확정 주문에 취소 시각을 넣는 변경을 거부합니다.

35강에서 이미 취소한 주문에는 실제 취소 시각을 알아낼 기록이 없습니다. V7은 그 값을 NULL로 두고 기존 상태·상품·금액을 보존합니다. 화면은 이 주문에 ‘취소 시각 기록 없음’을 표시합니다.

## 변경 이력과 스키마 대조

Spring Boot는 시작할 때 Flyway 마이그레이션을 실행합니다. Flyway는 `flyway_schema_history`에서 적용한 버전과 체크섬을 읽습니다. 기존 DB가 V6까지 적용된 상태라면 V7을 실행하고, 빈 DB라면 V1부터 V7까지 순서대로 실행합니다. 다시 시작하면 이미 성공한 버전은 다시 실행하지 않습니다. [Spring Boot DB 초기화](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Flyway 버전 변경 기록](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations)

이미 적용한 SQL 파일을 바꾸면 저장된 체크섬과 달라져 시작이 중단됩니다. 다음 변경은 V8처럼 새 파일로 추가합니다. `ddl-auto=validate`는 마이그레이션 이후 엔티티가 참조하는 테이블·열·자료형을 대조합니다.

새 DB와 기존 DB가 V7까지 도달하면 열·제약·인덱스 구조가 같아야 합니다. 데이터는 서로 다릅니다. 기존 DB에는 이전 회원과 주문이 남고, 새 DB에는 아직 회원과 주문이 없습니다.

## 볼륨과 로그인 상태

이 예제의 `postgres_data`는 이름이 있는 볼륨입니다. 같은 Compose 프로젝트 이름으로 다시 실행하면 같은 볼륨을 사용합니다. 프로젝트 이름을 바꾸면 별도의 볼륨을 만들므로 기존 데이터가 바로 나타나지 않습니다.

회원과 주문은 볼륨의 DB에 저장되고 로그인 세션은 앱 메모리에 있습니다. 앱 재생성 후 내 정보 요청이 401을 반환하면 다시 로그인합니다. 로그인 후 기존 주문이 보이면 DB 데이터가 유지된 것입니다.
