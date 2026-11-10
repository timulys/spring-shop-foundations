# 쇼핑몰의 용어와 코드 위치

| 용어·규칙 | 의미 | 담당 코드 |
|---|---|---|
| 상품 가격 | 지금 판매하는 상품의 원 단위 단가 | catalog.Product |
| 장바구니 | 주문 전 상품 코드와 수량, 변경 번호 | cart.Cart |
| 주문 당시 단가 | 주문을 만들 때 복사한 단가 | order.OrderItem |
| 주문 확정 | 상품 합계·할인·배송비를 계산하고 항목을 고정 | order.ShopOrder.confirm |
| 주문 취소 | 확정 주문의 상태를 바꾸고 원래 금액·항목을 보존 | order.ShopOrder.cancel |
| 공통 오류 응답 | HTTP 상태, 오류 코드, 요청 식별자 | common.ApiExceptionHandler |

catalog는 현재 상품 정보를, cart와 order는 구매 과정과 주문 기록을 담당합니다. 같은 상품 코드라도 현재 상품 가격과 주문 당시 단가는 서로 다른 의미를 가집니다. 이 의미와 규칙을 대화·문서·코드에서 같은 이름으로 표현합니다.

이번 단계는 하나의 애플리케이션 안에서 기능별 패키지를 정리합니다. cart와 order는 구매 과정의 서로 다른 책임을 묶은 패키지입니다. 바운디드 컨텍스트는 용어와 모델의 의미가 일관되는 범위이며, 패키지 개수나 서비스 개수만으로 정하지 않습니다.

## 현재 의존 관계

- cart.CartService → catalog.ProductService: 장바구니의 현재 상품 정보 조회
- order.OrderService → cart.CartRepository, catalog.ProductRepository: 주문 재료 조회
- order.ShopOrder와 OrderItem → catalog.Product: 주문 생성 시 정보 복사
- catalog·cart·order → common: 공통 오류 전달
- common은 catalog·cart·order의 업무 코드를 참조하지 않습니다.

상품 엔티티와 Repository를 직접 참조하는 연결은 아직 남아 있습니다. 후속 강의에서 상품 조회 계약과 정책을 배울 때 의존 방향을 더 정리합니다. 상품의 현재 가격이 바뀌어도 이미 저장한 주문 항목은 자신의 이름과 단가를 사용합니다.

## 파일 이동 확인

ShopApplication은 study.shop에 둡니다. 기능 패키지는 그 아래에 있으므로 기존 탐색 범위에 포함됩니다. 파일의 package와 import, 같은 패키지에서만 접근하던 생성자·메서드, JPQL 문자열 안의 DTO 전체 경로, 테스트 설정의 클래스 경로를 함께 확인합니다. DB 테이블과 HTTP 주소는 유지합니다.

CartItem의 수량 변경 메서드는 cart 안에서 사용합니다. OrderItem은 주문 수량을 직접 검증합니다. 두 기능은 현재 같은 1~99 범위를 사용하며, 주문의 검증 때문에 Cart의 내부 메서드를 공개하지 않습니다.

과제: 상품 단가를 바꿀 때 수정할 패키지와 기존 주문의 금액이 유지되는지 확인할 테스트를 찾아보세요. catalog/ProductRepositoryTest와 order/OrderQueryTest, order/OrderSnapshotTest를 참고하세요.
