package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeparticipant.enums.ParticipantStatus;
import java.util.UUID;

/**
 * 교환 참가자 한 명.
 *
 * <p>{@code dummy} 는 어드민이 만든 사용자인지를 뜻한다. 더미는 화면을 들고 있는 사람이 없어서
 * 스스로 수락할 수 없고, 어드민이 대신 눌러 줘야 흐름이 이어진다.
 */
public record ParticipantView(
        Long id, UUID userId, String shortId, String username, ParticipantStatus status, boolean dummy) {
}
