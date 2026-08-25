package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 목록 한 줄.
 *
 * <p>{@code shortId} 는 UUID 앞 8자리다. 부스에서 참가자와 화면을 맞춰 볼 때 36자를 다 읽을
 * 수는 없어서, 눈으로 대조할 수 있는 만큼만 보여 준다.
 */
public record UserView(
        UUID id,
        String shortId,
        String username,
        LocalDateTime createdAt,
        int haveCount,
        int wantCount,
        boolean online,
        boolean dummy) {

    public static UserView of(User user, int haveCount, int wantCount, boolean online) {
        return new UserView(
                user.getId(),
                user.getId().toString().substring(0, 8),
                user.getUsername(),
                user.getCreatedAt(),
                haveCount,
                wantCount,
                online,
                user.isAdminManaged());
    }
}
