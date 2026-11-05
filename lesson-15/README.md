# 15강 · 주문 목록을 정렬하고 페이지로 조회하기

`GET /api/orders?page=0&size=2`로 주문을 최신 생성 시각부터 조회합니다. 시각이 같으면 주문 번호가 큰 것부터 정렬합니다. 응답에는 주문 요약과 페이지 정보가 들어 있습니다.

## 실행

JDK 21과 Docker가 필요합니다. Spring Boot 4.1.2, Gradle Wrapper 8.14.3, PostgreSQL 18.4를 사용합니다. IntelliJ에서 이 폴더를 열고 Project SDK와 Gradle JVM을21로 지정합니다. 이전 강의의 앱과 DB컨테이너는 종료해 주세요.

```bash
docker compose up -d --wait
./gradlew bootRun
```

앱은 `http://localhost:8080`, DB는 `127.0.0.1:15432`입니다. 다른 포트는 `POSTGRES_PORT=15445 docker compose up -d --wait`와 앱의 `DB_URL=jdbc:postgresql://127.0.0.1:15445/shop`을 함께 사용합니다. 강의별 DB볼륨은 분리됩니다.

## 직접 작성하기

1. `OrderService.list()`의 `PageRequest.of()`에 페이지 번호·크기·정렬을 지정합니다.
2. `orders.findAll(pageable).map(OrderSummaryResponse::from)`으로 요약을 만듭니다.
3. 컨트롤러의 `@GetMapping`에서 page와 size를 받아 서비스에 전달합니다.

Pageable은 페이지 요청을 나타내는 인터페이스이고 PageRequest는 그 구현입니다. `findAll()`의 인자로 페이지 요청을 넘기면 DB조회에 범위가 적용됩니다. `Page`는 조회 내용뿐 아니라 전체 개수와 페이지 정보도 담습니다. 전체 개수를 얻기 위해 count쿼리가 실행될 수 있습니다. 목록 요약은 항목 컬렉션에 접근하지 않으며, 상품별 내역은 기존 상세 조회를 사용합니다.

## 요청과 결과

```bash
curl -s 'http://localhost:8080/api/orders?page=0&size=2'
curl -s 'http://localhost:8080/api/orders?page=1&size=2'
curl -s 'http://localhost:8080/api/orders?page=2&size=2'
curl -i 'http://localhost:8080/api/orders?page=-1&size=2'
curl -i 'http://localhost:8080/api/orders?size=51'
```

주문이 없으면 content는 빈 배열입니다. 기본값은 page=0, size=10입니다. 첫 페이지 번호는0이고 size는1~50입니다. page가 음수이거나 size가 범위를 벗어나거나 page×size가 정수 범위를 넘으면400 INVALID_PAGE입니다. 숫자로 변환할 수 없는 입력은400 HTTP_400입니다.

응답의 content에는 id·status·total·createdAt이 있습니다. number는 현재 페이지 번호, size는 요청한 페이지 크기, totalElements는 전체 주문 수, totalPages는 전체 페이지 수, hasNext는 다음 페이지 존재 여부입니다. 마지막 이후 페이지는200과 빈content를 반환합니다. 취소한 주문도 목록에 포함합니다.

실습용 주문은 빈 장바구니에 상품을 넣은 다음 생성합니다. 아래 두 요청을 다섯 번 반복하면 서로 다른 주문이 만들어집니다. 기존 장바구니가 있으면 먼저 항목을 정리하세요.

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H 'Content-Type: application/json' -d '{"productCode":"P001","quantity":2}'
curl -i -X POST http://localhost:8080/api/orders
```

각 주문의 총액은55000원입니다. 결제는 SIMULATED모의 승인이고 학습용 장바구니1번을 사용합니다. 회원별 접근 권한은 후속 인증 강의에서 추가합니다.

## 테스트

앱을 종료하고 DB만 켜 둡니다. 상품 가격은 기본값인 키보드30000원·마우스15000원이어야 합니다.

```bash
./gradlew test
./gradlew test --tests study.shop.OrderPaginationTest --rerun-tasks
```

전체36개 중 이번 페이지 테스트는8개입니다. 같은 시각의 주문5개로 정렬과 페이지 간 중복을 검사하고, 경계값·잘못된 입력·취소 상태·새 주문 삽입의 영향을 확인합니다. 테스트가 만든 주문만 삭제하며 이전 주문은 유지합니다.

정렬 기준은 고정된 데이터의 순서를 결정합니다. 요청 사이에 새 주문이 들어오면 OFFSET기준 위치가 이동하여 이미 본 주문이 다시 나타날 수 있습니다. 데이터가 바뀌는 동안의 순회에는 별도의 커서 방식 등 요구에 맞는 설계가 필요합니다. 큰OFFSET은 DB가 건너뛸 행도 처리하므로 비용이 커질 수 있습니다.

## 과제

size를2에서3으로 바꿨을 때 주문5개의 각 페이지 항목 수와 totalPages·hasNext를 예상하고 요청해 보세요. 데이터가 변하지 않으면0번은3개·hasNext=true,1번은2개·hasNext=false,전체2페이지입니다.

앱은Ctrl+C, DB는 `docker compose down`으로 종료합니다. 볼륨은 유지됩니다.
