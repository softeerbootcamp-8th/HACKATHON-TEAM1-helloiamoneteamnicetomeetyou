package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.ExchangeScoped;

/**
 * 약속 관련 실시간 이벤트에 실어 보내는 내용이다.
 *
 * <p>담긴 것은 교환 번호 하나뿐이다. 화면은 알림을 받으면 조회 API 로 현재 상태를 다시 읽는
 * 방식이라({@code PokeEventDto} 와 같은 이유다) 여기에 상태를 담을 이유가 없다.
 *
 * <p>예전에는 {@code Map.of("exchangeId", id)} 를 그대로 보냈다. 나가는 JSON 은 같지만, 그러면
 * 알림함이 교환 번호를 꺼내려고 payload 가 {@code Map} 인지 DTO 인지부터 가려야 한다.
 */
public record ExchangeEventDto(Long exchangeId) implements ExchangeScoped {}
