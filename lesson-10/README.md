# 10강 · 요청 본문 검증과 공통 오류 응답

상품 코드와 수량을 JSON으로 받아 장바구니에 담습니다. 상품 정보는 PostgreSQL에서 읽고, 장바구니 수량은 고정된 학습용 사용자 한 명의 메모리에 보관합니다. 앱을 재시작하면 장바구니는 비워집니다. DB 저장은 11강에서 추가합니다.

Java 21, Spring Boot 4.1.2, PostgreSQL 18.4를 사용합니다.

## 실행

이전 강의의 앱과 DB를 중지한 뒤 이 폴더를 IntelliJ에서 엽니다. Gradle JVM을 Java 21로 설정합니다. 이전 DB가 15432 포트를 사용한다면 해당 강의 폴더에서 `docker compose down`을 실행합니다. 이름 있는 볼륨은 보존됩니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

<http://localhost:8080/>에는 기존 상품 목록이 나옵니다. 이번 강의의 장바구니는 아래 API로 조작합니다. 초기 상품은 P001 키보드 30000원, P002 마우스 15000원, P003 노트 5000원입니다.

## 직접 작성할 부분

9강 코드에 `AddCartItemRequest`와 `CartController`를 추가하고, DTO의 검증과 컨트롤러의 `@Valid @RequestBody`를 연결합니다. 서비스의 합산 수량 검사와 `ApiError`, `ApiExceptionHandler`를 읽으며 오류 응답을 확인합니다. 이 폴더에는 비교할 최종 코드만 있습니다.

`StrictQuantityDeserializer`와 `RequestIdFilter`는 준비 코드입니다. 전자는 수량을 JSON 정수로 제한하고, 후자는 요청마다 UUID를 만들어 `X-Request-Id` 헤더와 오류 본문의 `requestId`에 넣습니다.

## 정상 요청

```bash
curl -s http://localhost:8080/api/cart
curl -i -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":1}'
curl -s http://localhost:8080/api/cart
```

응답은 200입니다. 키보드 수량 1, 단가 30000, 항목 금액 30000, 전체 수량 1, 소계 30000을 확인합니다. 가격은 서버의 상품 데이터로 계산합니다.

## 실패 요청과 상태 유지

위 명령의 quantity를 0과 100으로 바꾸면 400 `VALIDATION_ERROR`입니다. `fieldErrors`에 quantity와 최소·최대 수량 안내가 나옵니다. 수량 생략과 null은 필수값 오류, -1은 최소값 오류입니다.

```bash
curl -i -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":0}'
curl -i -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":1'
curl -i -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P999","quantity":1}'
```

잘못된 JSON 문법은 400 `INVALID_JSON`, 없는 상품은 404 `PRODUCT_NOT_FOUND`입니다. 문자열 `"1"`, 소수 `1.0`, 불리언, 배열, 객체, Integer 범위 초과 수량도 `INVALID_JSON`으로 거부합니다. 실패 뒤 GET으로 확인하면 기존 키보드 수량 1과 소계가 유지되어야 합니다.

## 합산 제한·수량 변경·삭제

처음 담은 1개에 98개를 추가하면 수량 99, 소계 2970000입니다. 여기서 1개를 더 추가하면 400 `QUANTITY_LIMIT`이며 수량은 99로 유지됩니다. 서로 다른 상품은 최대 100종입니다.

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' \
  -d '{"productCode":"P001","quantity":98}'
curl -s -X PATCH http://localhost:8080/api/cart/items/P001 \
  -H 'Content-Type: application/json' -d '{"quantity":2}'
curl -s -X DELETE http://localhost:8080/api/cart/items/P001
curl -s http://localhost:8080/api/cart
```

PATCH는 해당 수량으로 변경합니다. 기존 수량에 더하지 않습니다. DELETE는 항목을 지웁니다. 없는 장바구니 항목을 변경하거나 삭제하면 404입니다. 모든 정상 장바구니 응답은 200입니다.

## 규칙과 테스트

수량은 1~99이며, POST 요청 값과 기존 수량을 합친 값 모두 검사합니다. `@Min`·`@Max`는 null을 거부하지 않으므로 `@NotNull`을 함께 사용합니다. 상품 코드는 필수이며 공백만 있으면 거부하고 최대 20자로 제한합니다.

금액은 원 단위 `long`입니다. 단가 최대 1000000 × 수량 99 × 상품 100종 = 9900000000으로 int 범위를 넘을 수 있습니다. 장바구니를 읽을 때 현재 DB 가격을 사용하며 할인·배송비는 적용하지 않습니다. 단일 앱의 메모리 장바구니로, 로그인과 여러 서버의 상태 공유는 이후 단계에서 확장합니다.

DB를 켠 상태에서 `./gradlew test`를 실행하면 상품 저장·조회 테스트를 수행합니다. 장바구니 과제는 정상 1개를 담은 뒤 수량 생략·null·-1을 각각 보내고, 상태와 안내를 비교하는 것입니다.

실습을 마치면 앱을 중지하고 `docker compose down`으로 10강 컨테이너를 종료합니다. 공개 비밀번호는 로컬 실습용입니다.
