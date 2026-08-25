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
        /**
         * 식별 화면에서 쓸 표시와 번호. 시안의 "레몬 28" 이다.
         *
         * <p>참가자 전원이 같은 값을 든다. 같은 화면을 든 사람이 내 상대라는 것이 그 화면의
         * 규칙이라, 사람마다 다르면 서로를 못 찾는다. 진행 중인 다른 교환과도 겹치지 않는다.
         */
        int identityMark,
        int identityNumber,
        List<ExchangeParticipantResponseDto> participants,
        /** 모두가 되는 가장 빠른 칸. 없으면 null 이다. */
        Integer overlapSlot,
        /** 참가자 전원이 시간을 골랐는지. 화면의 "아직 상대방을 기다려야 해요" 가 이걸 본다. */
        boolean allAnswered,
        /** 확정된 만나는 시각. 아직이면 null 이다. */
        LocalDateTime confirmedTime) {
}
