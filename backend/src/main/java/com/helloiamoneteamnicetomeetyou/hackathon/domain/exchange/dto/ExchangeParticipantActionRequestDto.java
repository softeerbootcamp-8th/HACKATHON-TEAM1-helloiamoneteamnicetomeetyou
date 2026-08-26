package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import java.util.UUID;

/**
 * 참가자 본인이 매칭 결과에 수락하거나 거절할 때 쓴다.
 *
 * @param userId 요청을 보내는 본인. 이 교환의 참가자가 아니면 거부된다
 */
public record ExchangeParticipantActionRequestDto(UUID userId) {}
