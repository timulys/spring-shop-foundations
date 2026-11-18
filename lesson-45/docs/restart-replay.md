# 재시작 뒤 같은 주문 요청 보내기

전체 Compose 구성을 실행합니다. 아래 예는 curl을 사용하는 bash 명령이며, 다른 HTTP 도구에서도 같은 경로·헤더·본문을 사용할 수 있습니다. 값이 `응답에서 복사`로 표시된 곳에는 바로 앞 응답의 실제 값을 넣습니다. 실습용 쿠키 파일은 공유하거나 저장소에 올리지 않습니다.

## 회원가입과 로그인

사용하지 않은 아이디를 `lesson43_member` 대신 지정합니다. CSRF 응답의 `token`을 복사해 `CSRF`에 넣은 다음 가입과 로그인을 진행합니다.

```bash
BASE=http://127.0.0.1:8080
curl -c cookies.txt "$BASE/api/csrf"
CSRF='응답에서 복사'
curl -b cookies.txt -c cookies.txt -H "X-CSRF-TOKEN: $CSRF" -H 'Content-Type: application/json' -d '{"loginId":"lesson43_member","displayName":"실습 회원","password":"sample-pass-123"}' "$BASE/api/members"
curl -i -b cookies.txt -c cookies.txt -H "X-CSRF-TOKEN: $CSRF" -d 'username=lesson43_member&password=sample-pass-123' "$BASE/login"
curl -b cookies.txt -c cookies.txt "$BASE/api/csrf"
CSRF='로그인 후 받은 token'
```

로그인 응답은 204입니다. 로그인하면 CSRF 토큰이 바뀌므로 마지막 조회의 토큰을 사용합니다. 이 쿠키 파일은 브라우저 로그인과 별도의 세션입니다.

## 장바구니와 주문 생성

새 회원의 장바구니 버전은 0입니다. 첫 담기 응답은 `revision: 1`, 두 번째는 `revision: 2`입니다. 미리보기에서 70,000원을 확인하고 요청 키와 본문을 기록합니다.

```bash
curl -b cookies.txt -H "X-CSRF-TOKEN: $CSRF" -H 'X-Cart-Revision: 0' -H 'Content-Type: application/json' -d '{"productCode":"P001","quantity":2}' "$BASE/api/cart/items"
curl -b cookies.txt -H "X-CSRF-TOKEN: $CSRF" -H 'X-Cart-Revision: 1' -H 'Content-Type: application/json' -d '{"productCode":"P002","quantity":1}' "$BASE/api/cart/items"
curl -b cookies.txt "$BASE/api/orders/preview"
KEY=lesson43-restart-order-0001
curl -i -b cookies.txt -H "X-CSRF-TOKEN: $CSRF" -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' -d '{"cartRevision":2,"expectedTotalWon":70000}' "$BASE/api/orders"
```

201 응답의 주문 번호를 기록합니다. 같은 주문 명령을 그대로 반복하면 같은 번호를 반환합니다. `expectedTotalWon`만 70001로 바꾸면 409 `IDEMPOTENCY_KEY_REUSED`입니다.

## 주문 앱 재시작과 상품 앱 중단

```bash
docker compose stop catalog
docker compose restart order
docker compose up -d --wait order
```

메모리 세션이 사라졌으므로 같은 회원으로 다시 로그인합니다. 먼저 `/api/csrf`에서 새 쿠키와 토큰을 받고, 위의 로그인 명령을 실행한 뒤 CSRF 토큰을 한 번 더 갱신합니다. 가입과 장바구니 담기는 반복하지 않습니다.

기록한 `KEY`와 `cartRevision: 2`, `expectedTotalWon: 70000`으로 주문 명령을 다시 보냅니다. 상품 앱이 중단된 상태에서도 처음과 같은 주문 번호가 나와야 합니다. DB에서 해당 회원의 `orders`와 `order_requests`가 각각 한 건인지 확인합니다.

```bash
docker compose exec order-db psql -U shop -d shop -c "SELECT o.member_id, o.id, o.total_won, r.request_key FROM orders o JOIN order_requests r ON r.order_id=o.id WHERE r.request_key='lesson43-restart-order-0001';"
docker compose up -d --wait catalog
```

같은 키를 여러 회원이 사용했다면 회원별로 한 행씩 보입니다. 자신의 회원 번호와 주문 번호로 결과를 대조합니다. 실습이 끝나면 상품 앱을 복구하고 `cookies.txt`를 지웁니다.
