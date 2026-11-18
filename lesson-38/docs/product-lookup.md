# 상품 조회 계약과 구현

OrderService → ProductLookup ← DatabaseProductLookup → ProductRepository 순서로 연결합니다. 서비스가 아는 것은 계약과 ProductSnapshot이며 JPA 상품 엔티티는 DB 구현 안에서만 변환합니다.

ProductLookup.findByCode는 Optional<ProductSnapshot>을 반환합니다. 상품이 있으면 코드·이름·Money 단가를 전달하고, 없으면 Optional.empty입니다. 주문 서비스가 빈 결과를 PRODUCT_NOT_FOUND 오류로 바꿉니다. 분류 정보는 주문 계산에 사용하지 않으므로 스냅샷에 포함하지 않습니다.

ProductSnapshot은 order.domain에 둔 불변 데이터 전달 값입니다. 주문 항목도 이 값을 받아 이름과 단가를 복사합니다. record의 구성 요소와 Money가 모두 불변이므로 전달 뒤 값을 바꾸는 메서드가 없습니다. DB 엔티티와 생명주기를 공유하지 않습니다. 코드·이름의 빈 값과 길이, 1~1000000원 단가를 생성 시 검사합니다.

DatabaseProductLookup은 @Component로 등록합니다. 테스트의 @TestConfiguration은 @Primary 가짜 구현을 등록해 같은 OrderService의 생성자에 연결합니다. 가짜 구현은 테스트마다 Map을 초기화합니다. 실제 DB가 없어도 상품 값을 반환하지만 이 흐름 테스트는 장바구니와 주문을 저장하므로 PostgreSQL을 사용합니다.

고정 값이 같으면 미리보기와 저장은 70000원입니다. DB 단가를 990000원으로 바꾼 테스트에서도 가짜 구현은 30000원을 반환하며 주문 항목은 그 값을 보관합니다. 테스트가 끝나면 변경된 DB 값은 롤백됩니다. 조회 목록에서 P002를 제거한 뒤 주문하면 예외가 발생하고 저장 전 멈춥니다. 주문 수와 장바구니 2개 항목을 함께 검사합니다.

정책 분리와 조회 분리는 서로 다른 변경 이유를 다룹니다. DiscountPolicy는 금액 계산 방법을, ProductLookup은 필요한 상품 값을 얻는 방법을 바꿀 수 있게 합니다. 기본 앱 구성은 DB 조회와 기존 할인 정책을 사용합니다.
