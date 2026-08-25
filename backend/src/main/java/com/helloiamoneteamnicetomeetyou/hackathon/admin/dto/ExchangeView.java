package com.helloiamoneteamnicetomeetyou.hackathon.admin.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ExchangeView(
        Long id,
        ExchangeStatus status,
        String type,
        String zoneName,
        String boothName,
        LocalDateTime exchangeTime,
        LocalDateTime createdAt,
        List<ParticipantView> participants) {

    /** 하나라도 더미가 끼어 있으면 어드민이 대신 눌러 줘야 하는 교환이다. */
    public boolean hasDummy() {
        return participants.stream().anyMatch(ParticipantView::dummy);
    }
}
