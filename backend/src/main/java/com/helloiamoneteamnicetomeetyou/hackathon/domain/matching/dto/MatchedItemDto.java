package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/**
 * 매칭 화면에 카드 한 줄을 그리는 데 필요한 것만 담는다.
 *
 * <p>수량이 같이 붙는다. 같은 카드를 두 장 넘기는 것과 한 장 넘기는 것이 화면에서 구분되지
 * 않으면, 만나서 카드를 세는 자리에서야 어긋난 것을 알게 된다.
 *
 * <p>카드 자체는 {@link ItemResponseDto} 와 같은 필드를 담는다. 화면이 매칭 결과에서도 카드
 * 등록 화면과 같은 그림을 그리는데, 여기만 필드가 모자라면 그 카드만 약칭 없이 뜬다.
 *
 * @param description 한글 이름
 * @param code        카드 앞면 약칭. 이미지가 안 뜰 때 대신 보인다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchedItemDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        String code,
        Integer quantity) {

    public static MatchedItemDto from(ExchangeItem exchangeItem) {
        Item item = exchangeItem.getItem();
        return new MatchedItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getImageUrl(),
                ItemResponseDto.codeOf(item.getName()),
                exchangeItem.getQuantity());
    }
}
