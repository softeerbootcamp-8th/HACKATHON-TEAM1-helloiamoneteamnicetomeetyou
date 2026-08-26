package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;

/** 방금 보낸 찔러보기의 식별자. 화면이 응답 대기 상태를 이 값으로 잡아 둔다. */
public record PokeSendResponseDto(Long pokeId) {

    public static PokeSendResponseDto from(Poke poke) {
        return new PokeSendResponseDto(poke.getId());
    }
}
