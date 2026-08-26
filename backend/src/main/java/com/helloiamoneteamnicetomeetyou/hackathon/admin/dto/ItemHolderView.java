package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.ItemStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 이 카드를 내놓은 사람 한 명과 그 사람의 수량 상태.
 *
 * <p>카드 화면에서 "내놓은 사람이 셋인데 왜 매칭이 안 붙지" 를 푸는 데 필요하다. 셋 다
 * 예약됐거나 다 나갔으면 후보가 없는 것인데, 사람 이름만 보면 그것이 안 보인다.
 */
public record ItemHolderView(UserView user, Integer quantity, Integer quantityLeft, ItemStatus status) {

    public static ItemHolderView of(UserHaveItem have, Set<UUID> connected) {
        return new ItemHolderView(
                UserView.of(have.getUser(), List.of(), List.of(), connected.contains(have.getUser().getId())),
                have.getQuantity(),
                have.getQuantityLeft(),
                have.getStatus());
    }

    /** 지금 매칭에 잡힐 수 있는 카드인지. 예약됐거나 다 나갔으면 후보가 아니다. */
    public boolean available() {
        return status == ItemStatus.LEFT && quantityLeft != null && quantityLeft > 0;
    }

    /**
     * 화면에 적는 상태.
     *
     * <p><b>남은 개수가 0 이면 저장된 상태와 상관없이 "다 나감" 으로 적는다.</b> 예전 데이터에
     * {@code quantity_left = 0} 인데 {@code status} 가 {@code LEFT} 로 남아 있는 줄이 있는데,
     * 그대로 적으면 "교환 가능 · 남은 0" 이라는 앞뒤가 안 맞는 줄이 부스에서 보인다. 운영자가
     * 믿어야 하는 것은 남은 개수 쪽이다.
     */
    public String statusLabel() {
        if (quantityLeft != null && quantityLeft <= 0) {
            return "다 나감";
        }
        return status == ItemStatus.RESERVED ? "예약됨" : "교환 가능";
    }

    /** 배지 색을 고르는 자리. 글자와 같은 기준을 쓴다. */
    public String statusTone() {
        if (quantityLeft != null && quantityLeft <= 0) {
            return "out";
        }
        return status == ItemStatus.RESERVED ? "reserved" : "left";
    }
}
