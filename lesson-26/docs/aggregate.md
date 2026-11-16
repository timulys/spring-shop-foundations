# 주문의 변경 경계

`ShopOrder.draft()`로 임시 주문을 만들고 `addItem`, `confirm`, `cancel`로 변경합니다. 주문은 항목과 금액을 함께 관리하는 루트입니다. `OrderItem` 생성자는 같은 domain 패키지에서만 호출할 수 있고 외부 변경 메서드는 없습니다.

`getItems()`는 `List.copyOf`로 목록을 반환합니다. 호출자가 목록의 항목을 추가하거나 삭제할 수 없으며, 반환 이후 원본 목록에 추가한 항목도 기존 반환 목록에는 나타나지 않습니다. 요소 자체를 깊은 복사하는 기능은 아닙니다. 이 예제는 OrderItem에도 외부 변경 메서드를 두지 않아 확정된 주문 항목을 보호합니다.

DRAFT에서 confirm으로 CONFIRMED가 되고 CONFIRMED에서 cancel로 CANCELLED가 됩니다. 취소 뒤 재확정과 반복 취소는 ORDER_STATE_CONFLICT입니다. 취소해도 주문 당시 항목과 70,000원 계산 결과는 유지합니다. 공개 HTTP 취소는 확정된 주문만 허용합니다.

이 객체의 상태 검사와 DB 트랜잭션은 서로 다른 역할입니다. 여러 객체의 저장·장바구니 비우기와 롤백은 OrderService의 트랜잭션으로 묶습니다. 같은 주문 취소 요청은 Repository의 잠금으로 직렬화합니다.
