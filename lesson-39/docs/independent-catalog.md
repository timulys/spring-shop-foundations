# 별도로 실행하고 자기 DB만 사용하기

`catalog-service`는 상품 목록·검색·단일 조회와 내부 묶음 조회를 담당합니다. 상품 앱의 Flyway는 자기 DB에 `products`와 마이그레이션 이력 테이블만 만듭니다. 주문 앱의 소스나 빌드 산출물 없이 상품 앱 폴더만으로 JAR를 만들 수 있습니다.

## DB 접속 정보의 소유자

| 앱 | 접속 주소 | DB 사용자 | 마이그레이션 범위 |
|---|---|---|---|
| order | `order-db:5432/shop` | shop | 기존 상점 V1~V7 |
| catalog | `catalog-db:5432/catalog` | catalog | 상품 DB V1 |

두 PostgreSQL 컨테이너는 별도 데이터 볼륨과 계정을 사용합니다. 상품 DB에는 `shop` 사용자가 없고 주문 DB에는 `catalog` 사용자가 없습니다. 따라서 주문 DB의 계정으로 상품 DB에 접속하면 인증 단계에서 실패합니다. [PostgreSQL 연결 인증](https://www.postgresql.org/docs/18/client-authentication.html)

루트의 `.env`는 두 앱을 실행하는 Compose 설정입니다. 주문 컨테이너에는 주문 DB의 접속 정보만, 상품 컨테이너에는 상품 DB의 접속 정보만 전달합니다. 각 앱의 계정은 해당 실습 DB 초기화에 사용한 계정이며, 운영 배포에서 마이그레이션 계정과 실행 계정의 권한을 나누는 설계는 별도 항목입니다.

## 접속 실패를 직접 확인하기

기본 실습 비밀번호를 사용하는 경우, 루트에서 다음 명령으로 주문 DB의 계정을 사용해 상품 DB에 접속합니다.

```bash
docker compose exec -e PGPASSWORD=shop-local-only order-db psql -h catalog-db -U shop -d catalog -c 'select count(*) from products;'
```

인증 실패와 0이 아닌 종료 코드가 나옵니다. 상품 DB의 계정으로 자기 DB를 조회하면 상품 수 3이 나옵니다.

```bash
docker compose exec -e PGPASSWORD=catalog-local-only catalog-db psql -h 127.0.0.1 -U catalog -d catalog -c 'select count(*) from products;'
```

호스트에 DB 포트를 공개하지 않은 기본 Compose에서도 컨테이너 사이의 DNS 이름은 사용할 수 있습니다. 네트워크로 서버에 닿는 것과 DB 사용자 인증을 통과하는 것은 각각 필요한 조건입니다.

## 상품 앱만 중단하고 다시 시작하기

상점에서 주문을 하나 만든 뒤 상품 앱을 중단합니다.

```bash
docker compose stop catalog
docker compose ps
```

독립 상품 API는 접속할 수 없지만 주문 앱과 상점 화면은 계속 실행됩니다. 이미 만든 주문의 번호와 금액을 조회해 주세요. 이번 전환 단계의 상점은 로컬 상품 테이블을 사용하므로 현재 상품 목록도 계속 나옵니다. 40강에서 HTTP 조회를 연결하면 새 주문 계산은 상품 서비스의 응답에 의존하게 됩니다.

```bash
docker compose up -d --wait catalog
docker compose logs catalog
```

상품 앱이 준비되면 독립 상품 API가 다시 200을 반환합니다. 주문 앱을 재시작하지 않았으므로 로그인 세션과 주문 앱의 시작 시각은 그대로입니다. 각 서비스의 로그와 상태를 따로 확인할 수 있다는 점도 실행 단위를 나눈 결과입니다.

## 현재 단계에서 남은 전환

현재 두 DB에는 초기 상품 3개가 각각 들어 있습니다. 상품 수정 API는 없으며, 이 복사본은 독립 실행을 먼저 확인하기 위한 단계입니다. 다음 강의에서 주문 앱의 로컬 상품 조회를 제거하고, 내부 HTTP 묶음 조회로 같은 주문 금액과 스냅샷을 만들겠습니다. 브라우저의 상품 요청을 상품 앱으로 보내는 NGINX 경로 변경은 41강에서 진행합니다.
