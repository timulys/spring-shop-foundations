# 11강 · 장바구니와 항목을 관계로 저장하기

Java 21, Spring Boot 4.1.2, PostgreSQL 18.4를 사용합니다.
IntelliJ에서 이 폴더를 열고 Gradle JVM을 Java 21로 설정해 주세요.

## 실행

이전 강의의 앱과 DB 컨테이너를 종료한 뒤 실행합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

앱 주소는 http://localhost:8080 입니다. 상품 화면은 그대로 유지합니다.
11강 DB는 별도의 볼륨을 사용하며 처음 시작하면 상품 3개와 빈 장바구니 1번을 만듭니다.
로그인 도입 전 고정 학습용 사용자 한 명의 장바구니입니다.

## 직접 확인할 요청

아래 추가 요청을 두 번 보내면 P001 한 행의 수량이 2가 됩니다.
이미 담긴 항목이 있으면 먼저 GET으로 확인한 뒤 필요한 항목만 DELETE해 주세요.

```bash
curl -s http://localhost:8080/api/cart
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":1}'
curl -s -X PATCH http://localhost:8080/api/cart/items/P001 \
  -H 'Content-Type: application/json' -d '{"quantity":3}'
docker compose exec db psql -U shop -d shop -c \
  'SELECT id, cart_id, product_code, quantity FROM cart_items ORDER BY id;'
```

앱을 종료하고 다시 실행해도 GET 결과의 수량은 3입니다.

```bash
curl -s -X DELETE http://localhost:8080/api/cart/items/P001
docker compose exec db psql -U shop -d shop -c \
  'SELECT * FROM carts; SELECT * FROM cart_items; SELECT code FROM products ORDER BY code;'
```

선택한 cart_items 행만 없어지고 장바구니 부모와 상품 3개는 유지됩니다.
같은 장바구니의 같은 상품은 UNIQUE(cart_id, product_code)로 한 행만 허용합니다.
상품 코드는 문자열로 보관하며 상품과의 JPA 연관관계나 DB 외래 키는 만들지 않습니다.
상품의 존재와 현재 가격은 서비스에서 조회합니다.

## 따라 작성할 부분과 준비 코드

CartItem의 ManyToOne·JoinColumn, Cart의 OneToMany·mappedBy,
추가·수량 변경·삭제 메서드를 작성합니다. mappedBy의 cart는 자바 필드 이름입니다.
FK 컬럼은 cart_id이며 CartItem.cart가 연관관계의 소유 측입니다.
Cart에서 CartItem으로 새 항목 저장만 PERSIST로 전파합니다.
orphanRemoval은 관리 중인 부모의 컬렉션에서 제거한 기존 항목을 삭제합니다.
부모 자체를 삭제할 때도 그 부모의 항목이 삭제됩니다. 다른 장바구니나 상품은 대상이 아닙니다.

Flyway V2, Repository와 서비스의 트랜잭션·행 잠금은 실행 준비 코드입니다.
모든 변경 요청이 같은 부모 행을 잠그고 처리하므로 동시에 담아도 수량 갱신이 순서대로 적용됩니다.
트랜잭션의 커밋·롤백은 13강, 동시성 설계는 이후 강의에서 자세히 다룹니다.
읽기 결과는 DTO로 만들어 반환하며 엔티티를 JSON으로 직접 반환하지 않습니다.

## 테스트와 과제

DB를 실행한 상태에서 테스트합니다. 테스트용 변경은 기본적으로 롤백됩니다.
CartRelationTest는 각 테스트의 트랜잭션 안에서 장바구니를 비운 뒤 검사합니다.
테스트가 끝나면 이 준비 작업도 롤백되어 기존 장바구니가 유지됩니다.

```bash
./gradlew test
```

CartRelationTest는 FK, 같은 코드 합산, 선택한 자식 삭제, 다른 장바구니 보존,
99에 1을 더한 실패 후 수량 유지, 부모 삭제의 범위를 검사합니다.
과제는 P001의 수량을 99로 바꾸고 1을 추가해 400과 기존 수량 99를 확인하는 것입니다.

실습 후 앱을 종료하고 `docker compose down`으로 컨테이너를 종료합니다.
이 명령은 학습 데이터를 보관하는 볼륨을 유지합니다.
