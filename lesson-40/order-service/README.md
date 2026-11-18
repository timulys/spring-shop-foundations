# 주문 서비스 · HTTP 상품 조회

회원·장바구니·주문은 자기 DB에 저장하고, 현재 상품 정보는 상품 서비스의 API에서 읽습니다. `HttpProductLookup`이 장바구니의 상품 코드를 한 번에 보내 `ProductSnapshot`으로 변환합니다. 주문 DB에는 상품 테이블이 없습니다.

`CartStore`는 장바구니를 읽거나 짧게 변경하고, `OrderWriter`는 주문 저장과 장바구니 비우기를 하나의 로컬 트랜잭션으로 처리합니다. `CartService`와 `OrderService.place`·`preview`는 이 DB 작업 사이에서 원격 조회를 수행합니다.

실행·환경변수·검사 명령은 [루트 안내](../README.md)에 있습니다. `catalog` 패키지는 이번 강의의 브라우저 상품 URL을 유지하는 임시 HTTP 전달 코드입니다. 상품 엔티티나 DB 저장소는 포함하지 않으며, 41강의 NGINX 경로 전환 때 제거합니다.
