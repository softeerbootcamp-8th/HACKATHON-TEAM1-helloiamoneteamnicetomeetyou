package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import java.util.UUID;

/** 누가 눌렀는지만 필요한 요청(확정, 조율 요청)에 쓴다. */
public record ExchangeActorRequestDto(UUID userId) {
}
