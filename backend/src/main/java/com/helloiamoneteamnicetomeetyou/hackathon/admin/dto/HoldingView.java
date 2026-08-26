package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.ItemStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;

/**
 * 사용자가 들고 있는 카드 한 줄.
 *
 * <p>{@code quantity} 는 등록한 개수이고 {@code quantityLeft} 는 아직 남은 개수다. 둘이 갈리는
 * 것은 <b>매칭이 잡은 카드는 예약만 걸고 실제 개수는 교환이 끝날 때 줄기 때문</b>이다. 부스에서
 * "분명히 두 장 등록했는데 왜 매칭이 안 잡히지" 를 풀려면 남은 개수와 상태가 보여야 한다.
 *
 * <p>희망 카드는 이 값들이 없어서 전부 {@code null} 이다.
 */
public record HoldingView(Long id, ItemView item, Integer quantity, Integer quantityLeft, ItemStatus status) {

    public static HoldingView of(UserHaveItem have) {
        return new HoldingView(
                have.getId(),
                ItemView.of(have.getItem()),
                have.getQuantity(),
                have.getQuantityLeft(),
                have.getStatus());
    }

    public static HoldingView of(UserWantItem want) {
        return new HoldingView(want.getId(), ItemView.of(want.getItem()), null, null, null);
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
        if (status == null) {
            return null;
        }
        if (quantityLeft != null && quantityLeft <= 0) {
            return "다 나감";
        }
        return status == ItemStatus.RESERVED ? "예약됨" : "교환 가능";
    }

    /** 배지 색을 고르는 자리. 글자와 같은 기준을 쓴다. */
    public String statusTone() {
        if (status == null) {
            return null;
        }
        if (quantityLeft != null && quantityLeft <= 0) {
            return "out";
        }
        return status == ItemStatus.RESERVED ? "reserved" : "left";
    }

    /**
     * 등록한 개수와 남은 개수가 다른지. 다를 때만 화면에 둘 다 보여 준다.
     *
     * <p>늘 둘 다 적으면 "1 / 1" 이 줄마다 붙어서 다른 것이 눈에 안 띈다.
     */
    public boolean partlyGone() {
        return quantity != null && quantityLeft != null && !quantity.equals(quantityLeft);
    }
}
