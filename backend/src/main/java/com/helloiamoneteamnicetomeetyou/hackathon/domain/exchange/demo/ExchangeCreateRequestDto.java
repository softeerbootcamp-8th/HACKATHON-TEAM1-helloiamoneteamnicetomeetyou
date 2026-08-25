package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.demo;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import java.util.List;
import java.util.UUID;

/** 화면이 목업으로 찾은 매칭 결과를 그대로 실어 보낸다. 매칭이 서버로 오면 필요 없어진다. */
public record ExchangeCreateRequestDto(
        Long boothId,
        ExchangeType type,
        List<UUID> participantUserIds) {
}
