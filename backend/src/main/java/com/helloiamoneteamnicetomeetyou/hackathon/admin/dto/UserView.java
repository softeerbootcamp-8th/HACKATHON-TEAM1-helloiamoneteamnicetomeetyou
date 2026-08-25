package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 목록 한 줄.
 *
 * <p><b>카드를 숫자가 아니라 카드 자체로 들고 다닌다.</b> "내놓는 1 · 찾는 1" 이라고 적으면
 * 목록이 글자로 가득 차는데, 정작 부스에서 알고 싶은 것은 몇 장인지가 아니라 무엇을 들고
 * 있는지다. 화면은 이 목록으로 작은 카드 그림을 그린다.
 *
 * <p>{@code shortId} 는 UUID 앞 8자리다. 부스에서 참가자와 화면을 맞춰 볼 때 36자를 다 읽을
 * 수는 없어서, 눈으로 대조할 수 있는 만큼만 남긴다.
 */
public record UserView(
        UUID id,
        String shortId,
        String username,
        List<ItemView> haveItems,
        List<ItemView> wantItems,
        boolean online,
        boolean dummy) {

    public static UserView of(User user, List<ItemView> haveItems, List<ItemView> wantItems, boolean online) {
        return new UserView(
                user.getId(),
                user.getId().toString().substring(0, 8),
                user.getUsername(),
                haveItems,
                wantItems,
                online,
                user.isAdminManaged());
    }

    /** 이름이 없는 사용자는 앱에서 등록만 하고 아직 아무것도 안 한 사람이다. */
    public String displayName() {
        return (username == null || username.isBlank()) ? shortId : username;
    }
}
