# 도메인과 내부 역할

catalog는 현재 상품 정보를, cart는 담은 상품과 수량을, order는 주문 당시 정보와 금액·상태를 담당합니다. 각 도메인 안에 controller·service·domain·repository·dto를 배치합니다.

Controller는 요청과 응답을 연결합니다. Service는 조회·업무 처리·저장을 순서대로 실행하며 트랜잭션을 묶습니다. Domain 객체는 항목·금액·상태 규칙을 지킵니다. Repository는 DB를 조회하고 저장합니다. DTO는 HTTP 입력·응답에 필요한 값을 담습니다.

OrderService가 ShopOrder.draft()로 주문을 만들고 상품을 조회해 addItem을 호출합니다. confirm 뒤 확인 금액과 장바구니 변경 번호를 비교하고 주문을 저장한 다음 장바구니를 비웁니다. ShopOrder는 controller·service·HTTP DTO를 참조하지 않습니다. 현재 공통 ApiException 참조는 유지합니다. 상품 엔티티 대신 주문의 ProductSnapshot을 받습니다.

주문 루트와 OrderItem은 order.domain에 함께 두며, OrderItem 생성자는 패키지 내부에서만 접근할 수 있습니다. 공개 조작은 ShopOrder를 통합니다. Entity의 protected 기본 생성자는 JPA가 객체를 복원할 때 사용하고, 서비스는 draft라는 이름의 생성 메서드를 사용합니다.

JPQL의 생성자 표현식은 order.dto.OrderSummaryResponse의 전체 경로를 사용합니다. 단위 테스트는 domain에, 구매 흐름 테스트는 service에 배치합니다. 파일 이동 후 컴포넌트·엔티티·저장소 탐색과 실제 PostgreSQL 실행을 전체 테스트로 확인합니다.

27강은 order.domain에 DiscountPolicy·ThresholdFixedDiscountPolicy·NoDiscountPolicy를 추가합니다. order.service.OrderConfiguration은 기본 정책을 조립하고 OrderService는 계약을 생성자로 받습니다.

## 28강 조회 계약

order.service.port에 ProductLookup, order.infrastructure에 DatabaseProductLookup을 둡니다. 주문이 받는 ProductSnapshot은 order.domain의 불변 전달 값입니다. order.domain과 order.service는 catalog 엔티티·저장소를 직접 참조하지 않습니다. DB 구현이 변환 책임을 가집니다.
