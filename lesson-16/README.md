# 16강 · 연관 데이터 조회와 SQL 확인하기

주문 목록은 요약에 필요한 네 필드를 조회하고, 주문 상세는 주문과 항목을 함께 조회합니다. API 응답 형식과 금액 계산은 15강과 같습니다.

## 실행

JDK 21과 Docker가 필요합니다. Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을 21로 지정해 주세요. 이전 강의의 앱과 DB 컨테이너를 종료한 뒤 실행합니다.

```bash
docker compose up -d --wait
./gradlew bootRun --args='--spring.profiles.active=sql'
```

앱은 `http://localhost:8080`, DB는 `127.0.0.1:15432`입니다. `sql` 프로필로 실행하면 콘솔에 실제 Hibernate SQL이 표시됩니다. 기본 실행은 `./gradlew bootRun`입니다.

## 직접 작성하기

1. `OrderRepository.findSummaries()`에 네 필드의 DTO 생성자 쿼리와 개수 쿼리를 작성합니다.
2. `findDetailById()`에 `left join fetch o.items`를 넣습니다.
3. `OrderService`의 목록·상세 조회를 새 메서드에 연결합니다.

JPQL의 `ShopOrder`, `items`, `createdAt`은 자바 엔티티와 필드 이름입니다. `select new`에는 DTO의 전체 클래스 이름과 생성자 인자를 적습니다. 목록 조회에는 `Pageable`을 전달하고, 상세 조회는 주문 번호 하나로 범위를 정합니다.

## 요청과 SQL 비교

빈 장바구니에서 아래 요청으로 키보드 두 개와 마우스 한 개를 주문합니다.

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' -d '{"productCode":"P001","quantity":2}'
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' -d '{"productCode":"P002","quantity":1}'
curl -i -X POST http://localhost:8080/api/orders
curl -s 'http://localhost:8080/api/orders?page=0&size=2'
```

상세 주소는 생성 응답의 실제 주문 번호를 사용해 `/api/orders/{id}`로 요청합니다. 기본 상품 가격에서는 소계 75000원, 할인 5000원, 배송비 0원, 총액 70000원입니다. 상세 SQL에는 `order_items` 조인이 나타나고, 목록 데이터 SQL에는 `id`, `status`, `total_won`, `created_at`이 나타납니다. 전체 개수를 위한 count 쿼리는 페이지 내용에 따라 생략될 수 있습니다.

## 테스트

앱을 종료하고 DB만 켜 둡니다.

```bash
./gradlew test
./gradlew test --tests study.shop.OrderQueryTest --rerun-tasks
```

조회 테스트는 새 영속성 컨텍스트에서 실제 SQL을 기록합니다. 주문 세 개의 미초기화 항목을 차례로 읽는 예와 fetch join의 결과를 비교합니다. 목록은 응답의 동일성과 선택한 컬럼을 검사합니다. SQL 횟수는 이 테스트의 매핑·설정·데이터에서 측정한 값이며, 캐시·배치 조회·쿼리가 달라지면 함께 달라집니다.

LAZY는 연관 데이터의 로딩 시점에 관한 설정입니다. 초기화하지 않은 항목을 영속성 컨텍스트가 닫힌 뒤 읽는 실패와, 서비스 안에서 DTO를 완성하는 흐름도 테스트합니다. 실제 요청에서는 `spring.jpa.open-in-view=false`를 유지합니다.

## 과제

주문을 만든 뒤 현재 상품의 이름과 가격을 변경해 보세요. 주문 상세에는 주문 당시 이름과 가격이 남아야 합니다. 상세 조회 SQL에 상품 테이블이 포함되는지도 확인해 주세요. 수량·정렬·페이징·취소의 기존 테스트도 함께 통과해야 합니다.

앱은 Ctrl+C, DB는 `docker compose down`으로 종료합니다. DB 볼륨은 유지됩니다.
