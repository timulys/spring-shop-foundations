package study.shop.order.domain;

import study.shop.common.money.Money;
import study.shop.common.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrderAggregateTest {
    private final ProductSnapshot keyboard = new ProductSnapshot("P001", "키보드", Money.ofWon(30_000));
    private final ProductSnapshot mouse = new ProductSnapshot("P002", "마우스", Money.ofWon(15_000));

    @Test
    void onlyRootOperationsChangeOrderStateAndKeepAmounts() {
        ShopOrder order = ShopOrder.draft();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        order.addItem(keyboard, 2);
        order.addItem(mouse, 1);
        order.confirm(new ThresholdFixedDiscountPolicy());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotal()).isEqualTo(70_000);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(70_000);
    }

    @Test
    void callerCannotClearOrAppendToReturnedItems() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(keyboard, 2);
        List<OrderItem> exposed = order.getItems();
        assertThatThrownBy(exposed::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> exposed.add(exposed.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void previousItemListRemainsSnapshotWhenDraftGetsNewItem() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(keyboard, 1);
        List<OrderItem> firstRead = order.getItems();
        order.addItem(mouse, 1);
        assertThat(firstRead).hasSize(1);
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    void confirmedOrderRejectsAddedItemsAndReconfirmation() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(keyboard, 2);
        order.confirm(new ThresholdFixedDiscountPolicy());
        assertConflict(() -> order.addItem(mouse, 1));
        assertConflict(() -> order.confirm(new ThresholdFixedDiscountPolicy()));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotal()).isEqualTo(55_000);
    }

    @Test
    void cancelledOrderCannotBeConfirmedOrCancelledAgain() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(keyboard, 2);
        order.confirm(new ThresholdFixedDiscountPolicy());
        order.cancel();
        assertConflict(() -> order.confirm(new ThresholdFixedDiscountPolicy()));
        assertConflict(order::cancel);
        assertConflict(() -> order.addItem(mouse, 1));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(55_000);
    }

    @Test
    void emptyOrderCannotBeConfirmedAndRemainsDraft() {
        ShopOrder order = ShopOrder.draft();
        assertThatThrownBy(() -> order.confirm(new ThresholdFixedDiscountPolicy())).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("EMPTY_CART");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    void duplicateProductAndInvalidQuantityDoNotAddPartialItems() {
        ShopOrder order = ShopOrder.draft();
        order.addItem(keyboard, 1);
        assertThatThrownBy(() -> order.addItem(keyboard, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.addItem(mouse, 100)).isInstanceOf(ApiException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(1);
    }

    private static void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("ORDER_STATE_CONFLICT");
    }
}
