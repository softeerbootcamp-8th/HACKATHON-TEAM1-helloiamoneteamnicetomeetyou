package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.dto.ZoneResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 약속의 현재 상태 전부다. 화면은 실시간 알림을 받을 때마다 이걸 다시 읽어서 맞춘다.
 *
 * <p>{@code slotBaseTime} 과 {@code slotCount}, {@code slotMinutes} 를 함께 내려보내는 것이
 * 중요하다. 화면이 그리는 격자가 서버가 받아 주는 격자와 같아야 한다.
 */
public record ExchangeResponseDto(
        Long exchangeId,
        Long boothId,
        ExchangeType type,
        ExchangeStatus status,
        ZoneResponseDto zone,
        LocalDateTime slotBaseTime,
        int slotCount,
        int slotMinutes,
        /** 식별 화면에서 쓸 표시. 같은 교환의 참가자는 같은 값을 받는다. */
        int identityMark,
        List<ExchangeParticipantResponseDto> participants,
        /** 모두가 되는 가장 빠른 칸. 없으면 null 이다. */
        Integer overlapSlot,
        /** 참가자 전원이 시간을 골랐는지. 화면의 "아직 상대방을 기다려야 해요" 가 이걸 본다. */
        boolean allAnswered,
        /** 확정된 만나는 시각. 아직이면 null 이다. */
        LocalDateTime confirmedTime) {
}
