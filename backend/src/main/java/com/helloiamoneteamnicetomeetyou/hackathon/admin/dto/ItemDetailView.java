package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import java.util.List;

/**
 * 카드 한 장과 그 카드를 둘러싼 사람들.
 *
 * <p>카드 화면에서 알고 싶은 것은 "몇 명" 이 아니라 "누구" 다. 카드를 옮겨서 짝을 만들려면
 * 누구에게서 떼고 누구에게 붙일지 정해야 하는데, 숫자만 보면 사람 목록을 따로 뒤져야 한다.
 */
public record ItemDetailView(ItemView item, List<ItemHolderView> holders, List<UserView> seekers) {

    /**
     * 한쪽이 비면 이 카드는 아무리 기다려도 짝이 나지 않는다.
     *
     * <p>내놓은 사람이 있어도 전부 예약됐거나 다 나갔으면 마찬가지다. 사람 수만 세면
     * "셋이나 내놨는데 매칭이 안 붙는다" 가 왜인지 화면에서 알 수 없다.
     */
    public boolean isDeadEnd() {
        return holders.stream().noneMatch(ItemHolderView::available) || seekers.isEmpty();
    }

    /** 지금 실제로 교환에 쓸 수 있는 사람 수. 목록의 "내놓음" 숫자가 이걸 센다. */
    public long availableHolders() {
        return holders.stream().filter(ItemHolderView::available).count();
    }
}
