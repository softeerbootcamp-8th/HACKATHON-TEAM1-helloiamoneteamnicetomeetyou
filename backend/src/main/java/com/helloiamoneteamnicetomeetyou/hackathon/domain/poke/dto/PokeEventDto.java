package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.global.sse.ExchangeScoped;

/**
 * 실시간 알림에 실어 보내는 내용이다.
 *
 * <p><b>일부러 작게 둔다.</b> 서버는 끊긴 동안의 이벤트를 다시 보내지 않아서, 화면은 알림을
 * 받으면 조회 API 로 현재 상태를 다시 읽는 방식으로 맞춘다. 그래서 여기에 화면을 그릴 만큼
 * 담을 이유가 없고, 담으면 이벤트와 조회 응답 두 곳을 같이 고쳐야 한다.
 *
 * <p>{@code chosenItemId} 만 예외로 넣는다. 수락 알림을 받은 사람이 곧바로 "무엇을 주게
 * 됐는지" 를 보여 줘야 하는데, 그 값은 서버만 알기 때문이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PokeEventDto(Long pokeId, Long requestedItemId, Long chosenItemId, Long exchangeId)
        implements ExchangeScoped {

    public static PokeEventDto from(Poke poke) {
        return new PokeEventDto(
                poke.getId(),
                poke.getRequestedItem().getId(),
                poke.getChosenItem() == null ? null : poke.getChosenItem().getId(),
                poke.getExchange() == null ? null : poke.getExchange().getId());
    }
}
