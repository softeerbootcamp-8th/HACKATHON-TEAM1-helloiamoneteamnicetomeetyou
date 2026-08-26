package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내가 보낸 찔러보기 한 건.
 *
 * <p>쓰이는 곳이 둘이다. 대기 중({@code PENDING})인 상대의 카드를 화면에서 비활성화하는 데
 * 쓰고, 알림을 놓친 뒤 다시 붙었을 때 상태를 되살리는 데 쓴다.
 *
 * <p><b>{@code chosenItem} 을 반드시 여기서 받아 써야 한다.</b> 상대가 내 묶음에서 무엇을
 * 골랐는지는 서버만 안다. 화면이 짐작하면 찔러보기는 정의상 "상대 희망 ∩ 내 보유" 가 비어
 * 있어서 늘 엉뚱한 카드를 집는다.
 *
 * @param requestedItem 내가 상대에게 요청한 카드 (내가 받을 것)
 * @param chosenItem    상대가 내 묶음에서 고른 카드 (내가 줄 것). 수락 전에는 응답에서 빠진다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SentPokeResponseDto(
        Long pokeId,
        UUID targetUserId,
        String targetUserName,
        PokeStatus status,
        ItemResponseDto requestedItem,
        ItemResponseDto chosenItem,
        Long exchangeId,
        LocalDateTime createdAt,
        LocalDateTime respondedAt) {

    public static SentPokeResponseDto from(Poke poke) {
        return new SentPokeResponseDto(
                poke.getId(),
                poke.getToUser().getId(),
                poke.getToUser().getUsername(),
                poke.getStatus(),
                ItemResponseDto.from(poke.getRequestedItem()),
                poke.getChosenItem() == null ? null : ItemResponseDto.from(poke.getChosenItem()),
                poke.getExchange() == null ? null : poke.getExchange().getId(),
                poke.getCreatedAt(),
                poke.getRespondedAt());
    }
}
