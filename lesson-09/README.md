# 9강 · Repository로 상품 저장하고 조회하기

상품 API가 PostgreSQL의 `products`를 읽습니다. Java 21, Spring Boot 4.1.2, PostgreSQL 18.4를 사용합니다.

## 실행

이전 강의의 앱과 DB를 중지한 뒤 이 폴더를 IntelliJ에서 엽니다. Gradle JVM은 Java 21입니다. 15432 포트를 쓰던 이전 DB는 해당 강의 폴더의 `docker compose down`으로 종료합니다. 이름 있는 볼륨은 보존됩니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

브라우저: <http://localhost:8080/>. 처음에는 P001 키보드 30000원, P002 마우스 15000원, P003 노트 5000원이 나옵니다. 새 9강 볼륨에는 Flyway가 같은 V1을 적용합니다.

## 직접 작성할 부분

8강 최종 코드를 기반으로 `ProductRepository`를 추가하고, `ProductService`의 메모리 목록을 생성자로 받은 Repository로 바꿉니다. 이 폴더는 작성 결과를 비교할 최종 코드입니다.

`JpaRepository<Product, String>`의 첫 타입은 엔티티이고 두 번째는 식별자 타입입니다. `findById`는 결과가 없으면 빈 Optional을 반환합니다. 기존 컨트롤러의 `orElseThrow`가 P999 조회를 404로 바꿉니다.

목록은 `findAll(Sort.by("code"))`로 코드순 정렬한 뒤 기존 Java 필터를 적용합니다. 세 상품으로 조회 흐름을 배우는 단계이며, DB에서 조건·페이지를 처리하는 방식은 이후 강의에서 개선합니다. category·q의 빈 값 무시, q 최대 40자, maxPrice 0 이상, AND 조건을 유지합니다.

## 실제 DB 값과 API 비교

```bash
docker compose exec db psql -U shop -d shop
```

```sql
SELECT code, unit_price_won FROM products ORDER BY code;
UPDATE products SET unit_price_won = 32000 WHERE code = 'P001';
```

브라우저를 새로고침하면 키보드는 32000원입니다. <http://localhost:8080/api/products/P001>에서도 unitPrice가 32000입니다. 실습 뒤 아래 명령으로 복구합니다.

```sql
UPDATE products SET unit_price_won = 30000 WHERE code = 'P001';
SELECT code, unit_price_won FROM products ORDER BY code;
```

## 저장·조회·변경 테스트

DB를 실행해 둔 상태에서 `ProductRepositoryTest`를 IntelliJ의 테스트 실행 버튼으로 실행하거나 아래 명령을 사용합니다.

```bash
./gradlew test
```

P004 연필을 1000원으로 저장하고, DB를 다시 읽은 뒤 같은 코드의 가격을 1200원으로 바꿉니다. SQL 로그에는 조회와 INSERT, UPDATE가 나옵니다. flush는 SQL을 DB에 보내고 clear는 관리 중인 객체를 비워 다시 읽도록 합니다. 테스트의 `@Transactional`은 테스트 종료 후 변경을 롤백합니다. 정상 종료 후 원래 세 상품만 남습니다.

`save`의 반환 객체를 사용합니다. 이 예제는 null이 아닌 String ID를 직접 부여하고 버전 필드가 없어 기본 새 엔티티 판별에서 merge 경로를 사용합니다. 새 P004는 INSERT되고, 이미 있는 P004의 상태는 UPDATE됩니다. 이처럼 `save`는 엔티티 상태와 식별자에 따라 저장 결과가 달라집니다. 식별자 생성 방식과 트랜잭션의 상세 규칙은 이후 강의에서 확장합니다.

## 과제

`ProductController.product`의 빈 Optional 분기를 직접 작성합니다. P001은 200과 상품 JSON, P999는 404여야 합니다. 목록과 기존 쇼핑몰 화면도 그대로 동작해야 합니다.

앱을 중지하고 `docker compose down`으로 9강 컨테이너를 종료합니다. 공개된 비밀번호는 로컬 실습용입니다.
