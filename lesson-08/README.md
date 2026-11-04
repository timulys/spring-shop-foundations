# 8강 · 자바 객체를 JPA 엔티티로 매핑하기

`Product`의 필드를 `products` 테이블의 열과 연결합니다. Java 21, Spring Boot 4.1.2, PostgreSQL 18.4를 사용합니다.

## 실행

이전 강의의 DB가 15432 포트를 쓰고 있다면 그 강의 폴더에서 `docker compose down`을 실행합니다. 볼륨은 남습니다. 이 폴더를 IntelliJ에서 열고 Gradle JVM을 Java 21로 지정합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

첫 시작에는 Flyway가 `src/main/resources/db/migration/V1__create_products.sql`을 적용합니다. `products`와 세 상품을 만들고 `flyway_schema_history`에 적용 이력을 남깁니다. 이후 같은 DB로 시작하면 이미 적용한 V1을 다시 실행하지 않습니다. 8강 Compose 이름과 새 볼륨을 사용하므로 7강 데이터는 그대로 남습니다.

`Started ShopApplication`이 나오면 브라우저에서 <http://localhost:8080/>를 엽니다. **이 강의의 상품 API는 아직 `ProductService`의 메모리 목록을 읽습니다.** DB 조회로 바꾸는 작업은 9강에서 진행합니다.

## 매핑 확인

```bash
docker compose exec db psql -U shop -d shop
```

```sql
\d products
SELECT code, name, category, unit_price_won FROM products ORDER BY code;
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

상품은 P001 키보드 30000원, P002 마우스 15000원, P003 노트 5000원입니다. Flyway 이력은 버전 1, 성공 `t`입니다.

| Java | PostgreSQL |
|---|---|
| `code` (`@Id`, String) | `code varchar(20)`, 기본 키 |
| `name` (String) | `name varchar(100) NOT NULL` |
| `category` (String) | `category varchar(40) NOT NULL` |
| `unitPrice` (long) | `unit_price_won bigint NOT NULL` |

필드에 매핑 애너테이션을 두었으므로 Hibernate가 필드에 접근합니다. `protected Product()`는 JPA가 객체를 만들 때 사용할 기본 생성자입니다. 애플리케이션에서 직접 생성할 때는 기존 인자 생성자를 사용합니다. 상품 코드는 직접 정하므로 `@GeneratedValue`를 붙이지 않습니다.

`ddl-auto=validate`는 시작할 때 매핑과 테이블의 열·자료형 등을 검사합니다. 테이블을 자동 수정하지 않습니다. 모든 길이 제한이나 CHECK 조건을 검사하는 기능은 아니므로 실제 스키마와 입력 결과도 확인합니다.

## 상품명 길이 과제

`@Column(length = 100)`과 SQL의 `varchar(100)`을 비교합니다. 아래 첫 문장은 100자를 저장한 뒤 되돌립니다. 다음 문장은 101자이므로 DB가 거부합니다. 원래 세 상품은 유지됩니다.

```sql
BEGIN;
INSERT INTO products VALUES ('P004', repeat('가', 100), '문구', 1000);
SELECT char_length(name) FROM products WHERE code = 'P004';
ROLLBACK;
INSERT INTO products VALUES ('P004', repeat('가', 101), '문구', 1000);
SELECT count(*) FROM products;
```

이미 적용한 V1 파일은 이력과 함께 보존합니다. 실제 기존 DB의 구조를 바꾸는 절차는 이후 마이그레이션 강의에서 다룹니다. `db/` 아래의 SQL은 7강 복습 자료이며, 8강 초기화는 Flyway가 담당합니다.

## 종료

앱을 중지하고 `psql`은 `\q`로 나옵니다.

```bash
docker compose down
```

이 명령은 8강 컨테이너와 네트워크를 정리하고 이름 있는 볼륨은 남깁니다.
