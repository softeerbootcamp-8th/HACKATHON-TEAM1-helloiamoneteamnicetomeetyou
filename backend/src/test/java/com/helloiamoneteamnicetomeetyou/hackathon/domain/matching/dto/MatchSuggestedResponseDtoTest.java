package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.booth.entity.Booth;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.entity.Item;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 같은 교환을 받는 사람마다 다르게 정리해 보내는 부분이라, 방향이 틀리면 화면에 남의 카드가
 * 내 카드로 뜬다. 3인 교환은 눈으로 검산하기 어려워서 테스트로 고정한다.
 */
class MatchSuggestedResponseDtoTest {

    private final Booth booth = Booth.of("부스", "설명");

    private final User me = User.of(UUID.randomUUID(), "나");
    private final User b = User.of(UUID.randomUUID(), "비");
    private final User c = User.of(UUID.randomUUID(), "씨");

    private final Item myItem = Item.of(booth, "내카드", null);
    private final Item myOtherItem = Item.of(booth, "내다른카드", null);
    private final Item bItem = Item.of(booth, "비카드", null);
    private final Item cItem = Item.of(booth, "씨카드", null);

    @Test
    void 일대일은_내가_주는_카드와_받는_카드가_상대_한_명으로_모인다() {
        Exchange exchange = Exchange.create(ExchangeType.ONE_TO_ONE);
        List<ExchangeItem> items = List.of(
                ExchangeItem.create(exchange, me, myItem, b, 1),
                ExchangeItem.create(exchange, b, bItem, me, 1));

        MatchSuggestedResponseDto dto = MatchSuggestedResponseDto.of(exchange, items, me);

        assertThat(dto.type()).isEqualTo(ExchangeType.ONE_TO_ONE);
        assertThat(dto.giveItems()).extracting(MatchedItemDto::name).containsExactly("내카드");
        assertThat(dto.receiveItems()).extracting(MatchedItemDto::name).containsExactly("비카드");
        assertThat(dto.giveTo().id()).isEqualTo(b.getId());
        assertThat(dto.receiveFrom().id()).isEqualTo(b.getId());
        assertThat(dto.middleItems()).isEmpty();
    }

    /** 교환 수량이 2 이상이면 한쪽에 카드가 여러 장 묶인다. 한 장만 보내면 나머지가 조용히 사라진다. */
    @Test
    void 일대일에서_카드를_여러_장_주고받으면_전부_담긴다() {
        Exchange exchange = Exchange.create(ExchangeType.ONE_TO_ONE);
        List<ExchangeItem> items = List.of(
                ExchangeItem.create(exchange, me, myItem, b, 1),
                ExchangeItem.create(exchange, me, myOtherItem, b, 2),
                ExchangeItem.create(exchange, b, bItem, me, 3));

        MatchSuggestedResponseDto dto = MatchSuggestedResponseDto.of(exchange, items, me);

        assertThat(dto.giveItems()).extracting(MatchedItemDto::name).containsExactly("내카드", "내다른카드");
        assertThat(dto.giveItems()).extracting(MatchedItemDto::quantity).containsExactly(1, 2);
        assertThat(dto.receiveItems()).extracting(MatchedItemDto::name).containsExactly("비카드");
        assertThat(dto.receiveItems()).extracting(MatchedItemDto::quantity).containsExactly(3);
        assertThat(dto.middleItems()).isEmpty();
    }

    /** 고리는 나 → B → C → 나 다. 내 관점에서 giveTo 는 B, receiveFrom 은 C 여야 한다. */
    @Test
    void 삼인교환은_내가_주는_상대와_받는_상대가_갈리고_나머지_한_건이_middleItem_이_된다() {
        Exchange exchange = Exchange.create(ExchangeType.MULTI_WAY);
        List<ExchangeItem> items = List.of(
                ExchangeItem.create(exchange, me, myItem, b, 1),
                ExchangeItem.create(exchange, b, bItem, c, 1),
                ExchangeItem.create(exchange, c, cItem, me, 1));

        MatchSuggestedResponseDto dto = MatchSuggestedResponseDto.of(exchange, items, me);

        assertThat(dto.giveItems()).extracting(MatchedItemDto::name).containsExactly("내카드");
        assertThat(dto.giveTo().id()).isEqualTo(b.getId());
        assertThat(dto.receiveItems()).extracting(MatchedItemDto::name).containsExactly("씨카드");
        assertThat(dto.receiveFrom().id()).isEqualTo(c.getId());
        assertThat(dto.middleItems()).extracting(MatchedItemDto::name).containsExactly("비카드");
    }

    /** 같은 교환이라도 B 가 받는 payload 는 고리를 B 자리에서 본 것이어야 한다. */
    @Test
    void 삼인교환은_참여자마다_주고받는_카드가_다르게_나온다() {
        Exchange exchange = Exchange.create(ExchangeType.MULTI_WAY);
        List<ExchangeItem> items = List.of(
                ExchangeItem.create(exchange, me, myItem, b, 1),
                ExchangeItem.create(exchange, b, bItem, c, 1),
                ExchangeItem.create(exchange, c, cItem, me, 1));

        MatchSuggestedResponseDto forB = MatchSuggestedResponseDto.of(exchange, items, b);

        assertThat(forB.giveItems()).extracting(MatchedItemDto::name).containsExactly("비카드");
        assertThat(forB.giveTo().id()).isEqualTo(c.getId());
        assertThat(forB.receiveItems()).extracting(MatchedItemDto::name).containsExactly("내카드");
        assertThat(forB.receiveFrom().id()).isEqualTo(me.getId());
        assertThat(forB.middleItems()).extracting(MatchedItemDto::name).containsExactly("씨카드");
    }
}
