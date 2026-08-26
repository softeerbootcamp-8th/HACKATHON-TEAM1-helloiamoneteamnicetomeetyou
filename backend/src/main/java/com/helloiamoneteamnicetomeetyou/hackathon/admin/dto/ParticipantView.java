package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums.ParticipantStatus;
import java.util.List;
import java.util.UUID;

/**
 * 교환 참가자 한 명.
 *
 * <p>{@code dummy} 는 어드민이 만든 사용자인지를 뜻한다. 더미는 화면을 들고 있는 사람이 없어서
 * 스스로 수락할 수 없고, 어드민이 대신 눌러 줘야 흐름이 이어진다.
 *
 * <p>{@code slots} 는 이 사람이 고른 시간 칸이다. 더미 줄만 어드민이 고칠 수 있고, 실제 참가자
 * 줄은 그 사람이 자기 화면에서 고른 것이라 읽기만 한다.
 */
public record ParticipantView(
        Long id,
        UUID userId,
        String shortId,
        String username,
        ParticipantStatus status,
        boolean dummy,
        List<Integer> slots) {

    public String statusLabel() {
        return status.getLabel();
    }

    /** 이름이 없으면 UUID 앞자리로 부른다. 목록에서 빈칸이 보이면 누구인지 알 수 없다. */
    public String displayName() {
        return (username == null || username.isBlank()) ? shortId : username;
    }

    /** 템플릿에서 칸 하나가 칠해졌는지 묻는 자리다. */
    public boolean picked(int slotIndex) {
        return slots.contains(slotIndex);
    }
}
