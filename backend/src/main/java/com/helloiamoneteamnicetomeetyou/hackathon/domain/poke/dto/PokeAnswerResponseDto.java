package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;

/**
 * 찔러보기에 답한 결과다.
 *
 * <p>수락이면 만들어진 교환과 오갈 카드를 함께 준다. 답한 사람(받은 쪽) 기준이라
 * {@code giveItemId} 는 상대가 요청했던 내 카드이고 {@code receiveItemId} 는 내가 고른 카드다.
 *
 * <p>거절이면 교환도 카드도 없어서 두 값이 응답에서 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PokeAnswerResponseDto(
        Long pokeId,
        PokeStatus status,
        Long exchangeId,
        Long giveItemId,
        Long receiveItemId) {

    public static PokeAnswerResponseDto from(Poke poke) {
        if (poke.getStatus() != PokeStatus.ACCEPTED) {
            return new PokeAnswerResponseDto(poke.getId(), poke.getStatus(), null, null, null);
        }

        return new PokeAnswerResponseDto(
                poke.getId(),
                poke.getStatus(),
                poke.getExchange().getId(),
                poke.getRequestedItem().getId(),
                poke.getChosenItem().getId());
    }
}
