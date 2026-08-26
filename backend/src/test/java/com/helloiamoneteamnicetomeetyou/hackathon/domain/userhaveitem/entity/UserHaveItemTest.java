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

    @Test
    @DisplayName("교환으로 받기만 한 카드는 저절로 후보가 되지 않는다")
    void 받기만_한_카드는_후보가_아니다() {
        UserHaveItem acquired = UserHaveItem.acquired(user, item, 2);

        assertThat(acquired.getQuantity()).isEqualTo(2);
        assertThat(acquired.getQuantityLeft()).isZero();
        assertThat(acquired.getRegisteredQuantity()).isZero();
        assertThat(acquired.getStatus()).isEqualTo(ItemStatus.OUT);
    }

    @Test
    @DisplayName("받은 카드도 등록 화면에서 직접 등록하면 그때 후보가 된다")
    void 받은_카드를_직접_등록하면_후보가_된다() {
        UserHaveItem acquired = UserHaveItem.acquired(user, item, 2);

        acquired.changeQuantity(2);

        assertThat(acquired.getQuantityLeft()).isEqualTo(2);
        assertThat(acquired.getStatus()).isEqualTo(ItemStatus.LEFT);
    }

    @Test
    @DisplayName("이미 내놓고 있는 카드를 교환으로 더 받아도 내놓는 개수는 그대로다")
    void 더_받아도_내놓는_개수는_그대로다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 2);

        haveItem.receiveMore(1);

        assertThat(haveItem.getQuantity()).isEqualTo(3);
        assertThat(haveItem.getQuantityLeft()).isEqualTo(2);
    }

    @Test
    @DisplayName("묶여 있는 동안 내놓을 개수를 바꿔도 묶인 몫은 건드리지 않는다")
    void 묶인_몫은_등록으로_못_바꾼다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);
        haveItem.reserve(1);

        haveItem.changeQuantity(1);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(1);
        assertThat(haveItem.getReservedQuantity()).isEqualTo(1);
        assertThat(haveItem.getQuantity()).isEqualTo(2);
        assertThat(haveItem.isReserved()).isTrue();
    }

    @Test
    @DisplayName("등록을 0 으로 내려도 묶인 몫은 남는다")
    void 등록을_0_으로_내려도_묶인_몫은_남는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 2);
        haveItem.reserve(1);

        haveItem.changeQuantity(0);

        assertThat(haveItem.getQuantityLeft()).isZero();
        assertThat(haveItem.getReservedQuantity()).isEqualTo(1);
        assertThat(haveItem.getStatus()).isEqualTo(ItemStatus.RESERVED);
    }

    @Test
    @DisplayName("남은 것보다 많이 예약할 수 없다")
    void 남은_것보다_많이_예약하지_않는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 2);

        haveItem.reserve(5);

        assertThat(haveItem.getQuantityLeft()).isZero();
        assertThat(haveItem.getReservedQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("묶어 둔 것보다 많이 풀 수 없다")
    void 묶어_둔_것보다_많이_풀지_않는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 3);
        haveItem.reserve(1);

        haveItem.cancelReservation(5);

        assertThat(haveItem.getQuantityLeft()).isEqualTo(3);
        assertThat(haveItem.getReservedQuantity()).isZero();
    }

    @Test
    @DisplayName("묶인 것 없이 완료가 들어와도 보유 개수가 음수로 내려가지 않는다")
    void 묶인_것_없는_완료는_아무것도_깎지_않는다() {
        UserHaveItem haveItem = UserHaveItem.of(user, item, 1);

        haveItem.completeExchange(1);

        assertThat(haveItem.getQuantity()).isEqualTo(1);
        assertThat(haveItem.getQuantityLeft()).isEqualTo(1);
    }
}
