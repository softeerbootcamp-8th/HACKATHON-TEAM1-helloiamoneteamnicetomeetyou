package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import java.util.List;
import java.util.UUID;

/**
 * 내가 고른 칸 전부다. 부분 변경이 아니라 통째로 덮어쓴다.
 *
 * <p>칸 하나를 켜고 끄는 요청으로 두면 화면과 서버의 선택이 어긋났을 때 되돌릴 방법이 없다.
 * 매번 전체를 보내면 마지막 요청이 항상 옳다.
 */
public record TimeSlotUpdateRequestDto(UUID userId, List<Integer> slots) {
}
