package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("보유 카드의 예약과 완료")
class UserHaveItemTest {

    private final User user = User.of(UUID.randomUUID());
    private final Item item = mock(Item.class);

    @Test
    @DisplayName("3개 중 1개만 예약해도 나머지 2개는 계속 후보로 남는다")
    void 일부만_예약하면_나머지는_남는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);

        haveItem.reserve(1);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(2);
        assertThat(haveItem.isReserved()).isTrue();
    }

    @Test
    @DisplayName("완료되면 예약 시점에 이미 줄어든 개수를 또 깎지 않는다")
    void 완료는_예약된_개수를_다시_깎지_않는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);
        haveItem.reserve(1);

        haveItem.completeExchange(1);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(2);
        assertThat(haveItem.getStatus()).isEqualTo(ItemStatus.LEFT);
    }

    @Test
    @DisplayName("마지막 한 개까지 완료되면 OUT 이 된다")
    void 마지막_개수가_완료되면_OUT() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 1);
        haveItem.reserve(1);

        haveItem.completeExchange(1);

        assertThat(haveItem.getQuantityLeft()).isZero();
        assertThat(haveItem.getStatus()).isEqualTo(ItemStatus.OUT);
    }

    @Test
    @DisplayName("취소되면 예약해 뒀던 만큼 다시 후보로 돌아온다")
    void 취소하면_예약분을_되돌린다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);
        haveItem.reserve(1);

        haveItem.cancelReservation(1);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(3);
        assertThat(haveItem.isReserved()).isFalse();
    }

    @Test
    @DisplayName("완료되면 넘긴 만큼 등록해 둔 총 수량도 줄어든다")
    void 완료는_총_수량도_깎는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);
        haveItem.reserve(1);

        haveItem.completeExchange(1);

        assertThat(haveItem.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("다 넘긴 카드를 같은 개수로 다시 등록하면 그만큼 다시 내줄 수 있다")
    void 다_넘긴_카드를_다시_등록할_수_있다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 2);
        haveItem.reserve(2);
        haveItem.completeExchange(2);

        haveItem.changeQuantity(2);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(2);
        assertThat(haveItem.getStatus()).isEqualTo(ItemStatus.LEFT);
    }

    @Test
    @DisplayName("일부만 넘긴 뒤 남은 개수로 다시 등록해도 그 개수가 그대로 남는다")
    void 일부만_넘긴_뒤_다시_등록해도_남은_개수가_유지된다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 2);
        haveItem.reserve(1);
        haveItem.completeExchange(1);

        haveItem.changeQuantity(1);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(1);
        assertThat(haveItem.getStatus()).isEqualTo(ItemStatus.LEFT);
    }
}
