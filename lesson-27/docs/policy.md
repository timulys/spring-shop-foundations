# 할인 정책과 서비스

DiscountPolicy.calculateDiscount는 Money 상품 합계를 받아 Money 할인액을 반환합니다. 기본 정책은 50000원 이상에서 5000원을 반환하고 NoDiscountPolicy는 0원을 반환합니다. Money가 음수 생성을 막고 ShopOrder가 할인액의 null·상품 합계 초과를 막습니다. 잘못된 정책 결과가 나오면 DRAFT와 기존 항목을 유지하며 확정 필드에 일부 금액을 쓰지 않습니다.

OrderService는 CartRepository·ProductRepository·OrderRepository·DiscountPolicy를 생성자로 받습니다. 조회한 상품으로 주문을 만들고 confirm(discountPolicy)을 호출합니다. 장바구니 변경 번호·예상 금액 검사, 주문 저장, 장바구니 비우기의 순서는 유지합니다. OrderConfiguration은 기본 정책 하나를 빈으로 등록합니다. 도메인 정책 두 클래스에는 Spring 어노테이션이 없습니다.

배송비는 ShopOrder의 할인 전 합계로 결정합니다. 합계 50000원에서 기본 할인 적용 후 45000원이 되어도 배송비는 0원입니다. 할인 없음을 선택해도 합계 49999원에는 배송비 3000원이 붙습니다. 할인 정책은 배송비 정책을 대신하지 않습니다.

NoDiscountOrderFlowTest의 @TestConfiguration·@Primary는 테스트 실행에서만 다른 정책을 선택합니다. 같은 OrderService로 75000원 확인·주문·장바구니 비우기·조회·취소를 수행합니다. 앱의 기본 정책 선택은 바뀌지 않습니다. 정책 구현을 런타임에 임의 사용자 요청으로 고르게 하는 API는 이 강의 범위에 없습니다.

저장한 주문은 이미 선택된 할인액과 금액을 보관합니다. 이후 정책을 바꿔도 과거 주문을 읽거나 취소할 때 재계산하지 않습니다. 기존 자바 과정의 인터페이스와 구현 교체 개념을, Money와 Spring 생성자 주입이 있는 현재 프로젝트에 적용했습니다.
