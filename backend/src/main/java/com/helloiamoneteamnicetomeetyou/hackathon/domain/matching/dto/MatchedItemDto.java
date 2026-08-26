package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;

/**
 * 매칭 화면에 카드 한 줄을 그리는 데 필요한 것만 담는다.
 *
 * <p>수량이 같이 붙는다. 같은 카드를 두 장 넘기는 것과 한 장 넘기는 것이 화면에서 구분되지
 * 않으면, 만나서 카드를 세는 자리에서야 어긋난 것을 알게 된다.
 *
 * <p>카드 앞면의 약칭과 한글명은 아직 {@code Item} 에 없다. 컬럼을 늘릴지는 정해지지 않아서
 * 지금 있는 것만 내려보낸다. 정해지면 여기와 {@code Item} 을 같이 고친다.
 */
public record MatchedItemDto(Long id, String name, String imageUrl, Integer quantity) {

    public static MatchedItemDto from(ExchangeItem exchangeItem) {
        Item item = exchangeItem.getItem();
        return new MatchedItemDto(item.getId(), item.getName(), item.getImageUrl(), exchangeItem.getQuantity());
    }
}
