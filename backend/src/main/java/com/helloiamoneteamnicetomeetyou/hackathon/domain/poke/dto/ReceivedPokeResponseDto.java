package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.entity.Poke;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 내가 받은 찔러보기 한 건. 신청 수신 화면이 이걸로 그려진다.
 *
 * @param pokeId        응답할 때 쓰는 식별자
 * @param fromUserId    찔러본 사람
 * @param fromUserName  표시 이름. 아직 채우지 않는 값이라 보통 응답에서 빠진다
 * @param requestedItem 상대가 원하는 카드. 내가 가진 것이다 ("상대가 원하는 카드")
 * @param offeredItems  상대가 내놓은 카드 묶음. 이 중 한 장을 고른다 ("상대의 카드 묶음")
 * @param createdAt     받은 시각
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceivedPokeResponseDto(
        Long pokeId,
        UUID fromUserId,
        String fromUserName,
        ItemResponseDto requestedItem,
        List<ItemResponseDto> offeredItems,
        LocalDateTime createdAt) {

    /**
     * @param offered 보낸 사람의 <b>현재</b> 보유 카드. 저장해 둔 값이 아니라 조회 시점에 읽은
     *                것이라, 그 사이 재고가 바뀐 것이 그대로 반영된다
     */
    public static ReceivedPokeResponseDto of(Poke poke, List<UserHaveItem> offered) {
        return new ReceivedPokeResponseDto(
                poke.getId(),
                poke.getFromUser().getId(),
                poke.getFromUser().getUsername(),
                ItemResponseDto.from(poke.getRequestedItem()),
                offered.stream().map(have -> ItemResponseDto.from(have.getItem())).toList(),
                poke.getCreatedAt());
    }
}
