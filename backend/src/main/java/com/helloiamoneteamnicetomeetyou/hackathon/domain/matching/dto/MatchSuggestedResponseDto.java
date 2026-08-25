package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.entity.Exchange;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.enums.ExchangeType;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.exchangeitem.entity.ExchangeItem;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.user.entity.User;
import java.util.List;
import java.util.function.Predicate;

/**
 * MATCH_SUGGESTED 이벤트로 전달되는 매칭 결과 DTO.
 *
 * 수신자 기준으로 가공된 데이터를 내려보낸다.
 * 각 참여자에게 동일한 데이터를 보내고 클라이언트에서 직접 해석하게 하지 않고,
 * 서버가 "내가 누구에게 주고 누구에게 받는지"를 정리해서 전달한다.
 *
 * 3인 교환은 다음과 같은 순환 구조를 가진다.
 *
 *   나 → giveTo → receiveFrom → 나
 *
 * 필드 의미:
 * - giveTo       : 내가 아이템을 주는 사용자
 * - receiveFrom  : 내가 아이템을 받는 사용자
 * - middleItems  : giveTo → receiveFrom 구간에서 오가는 아이템
 *
 * 1:1 교환에서는 giveTo 와 receiveFrom 이 동일한 사용자이며,
 * middleItems 는 빈 목록이다.
 *
 * 주고받는 아이템은 여러 종류가 동시에 교환될 수 있으므로
 * giveItems, receiveItems, middleItems 는 모두 목록으로 표현한다.
 *
 * 반면 교환 상대는 방향별로 항상 한 명이므로
 * giveTo 와 receiveFrom 은 단일 사용자로 표현한다.
 */
public record MatchSuggestedResponseDto(
        Long exchangeId,
        ExchangeType type,
        List<MatchedItemDto> giveItems,
        MatchedUserDto giveTo,
        List<MatchedItemDto> receiveItems,
        MatchedUserDto receiveFrom,
        List<MatchedItemDto> middleItems
) {

    /**
     * 한 사람이 받을 payload 를 만든다.
     *
     * @param viewer 이 payload 를 받을 사람. 주고받는 방향이 이 사람 기준으로 정해진다
     */
    public static MatchSuggestedResponseDto of(Exchange exchange, List<ExchangeItem> items, User viewer) {
        List<ExchangeItem> gives = filter(items, item -> item.getFromUser().getId().equals(viewer.getId()));
        List<ExchangeItem> receives = filter(items, item -> item.getToUser().getId().equals(viewer.getId()));
        List<ExchangeItem> middles = filter(items, item -> !gives.contains(item) && !receives.contains(item));

        return new MatchSuggestedResponseDto(
                exchange.getId(),
                exchange.getType(),
                toDtos(gives),
                MatchedUserDto.from(gives.getFirst().getToUser()),
                toDtos(receives),
                MatchedUserDto.from(receives.getFirst().getFromUser()),
                toDtos(middles)
        );
    }

    private static List<ExchangeItem> filter(List<ExchangeItem> items, Predicate<ExchangeItem> match) {
        return items.stream().filter(match).toList();
    }

    private static List<MatchedItemDto> toDtos(List<ExchangeItem> items) {
        return items.stream().map(MatchedItemDto::from).toList();
    }
}
