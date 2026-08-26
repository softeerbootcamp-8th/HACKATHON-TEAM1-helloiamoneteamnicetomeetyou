package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import java.util.List;

/**
 * 카드 한 장과 그 카드를 둘러싼 사람들.
 *
 * <p>카드 화면에서 알고 싶은 것은 "몇 명" 이 아니라 "누구" 다. 카드를 옮겨서 짝을 만들려면
 * 누구에게서 떼고 누구에게 붙일지 정해야 하는데, 숫자만 보면 사람 목록을 따로 뒤져야 한다.
 */
public record ItemDetailView(ItemView item, List<UserView> holders, List<UserView> seekers) {

    /** 한쪽이 비면 이 카드는 아무리 기다려도 짝이 나지 않는다. */
    public boolean isDeadEnd() {
        return holders.isEmpty() || seekers.isEmpty();
    }
}
