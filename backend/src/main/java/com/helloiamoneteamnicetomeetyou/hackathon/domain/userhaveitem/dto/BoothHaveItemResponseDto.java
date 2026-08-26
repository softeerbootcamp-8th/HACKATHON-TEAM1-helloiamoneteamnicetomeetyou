package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.item.dto.ItemResponseDto;
import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;
import java.util.List;
import java.util.UUID;

/**
 * 부스 안 다른 사용자가 내놓은 카드 한 줄.
 *
 * <p>화면은 이 한 줄로 상태 배지까지 그린다. 판정 기준은 시안(desc 204:4948)이다.
 *
 * <ul>
 *   <li>매칭됨 — {@code matched} 다. 자동 매칭이 성사됐거나 찔러보기가 수락돼서 이 사람과
 *       진행 중인 교환이 있다
 *   <li>교환 가능 — {@code givableItemNames} 가 비어 있지 않다
 *   <li>그래도 찔러보기 — {@code givableItemNames} 가 비어 있다
 * </ul>
 *
 * <p>세 가지는 위에서부터 순서대로 본다. 매칭된 상대에게 줄 카드가 있어도 배지는 "매칭됨" 이다.
 *
 * @param haveItemId           보유 등록 줄의 식별자. 페이지 경계를 고정하는 정렬 기준이다
 * @param ownerId              이 카드를 가진 사람. 찔러보기의 {@code targetUserId} 가 이 값이다
 * @param ownerName            표시 이름. 아직 채우지 않는 값이라 보통 응답에서 빠진다
 * @param item                 카드. 찔러보기의 {@code requestedItemId} 가 {@code item.id} 다
 * @param quantity             주인이 내놓은 개수
 * @param wanted               내 희망 카드인가. 희망 카드를 하나도 등록하지 않았으면 전부 false 다
 * @param matched              이 사람과 지금 진행 중인 교환이 있는가
 * @param givableItemNames     그 주인에게 내가 줄 수 있는 카드 이름들 (상대 희망 ∩ 내 보유)
 * @param ownerWantedItemNames 그 주인이 원하는 카드 이름들. 화면의 "상대방이 원하는 것" 이다
 */
// CommonResponse 의 @JsonInclude 는 중첩된 이 DTO 까지 내려오지 않아서 여기에도 붙인다.
// 없으면 아직 비어 있는 ownerName 이 null 로 실려 나간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BoothHaveItemResponseDto(
        Long haveItemId,
        UUID ownerId,
        String ownerName,
        ItemResponseDto item,
        Integer quantity,
        boolean wanted,
        boolean matched,
        List<String> givableItemNames,
        List<String> ownerWantedItemNames) {

    public static BoothHaveItemResponseDto of(
            UserHaveItem haveItem,
            boolean wanted,
            boolean matched,
            List<String> givableItemNames,
            List<String> ownerWantedItemNames) {

        return new BoothHaveItemResponseDto(
                haveItem.getId(),
                haveItem.getUser().getId(),
                haveItem.getUser().getUsername(),
                ItemResponseDto.from(haveItem.getItem()),
                haveItem.getQuantity(),
                wanted,
                matched,
                givableItemNames,
                ownerWantedItemNames);
    }

    /** 줄 수 있는 카드가 있으면 교환이 바로 성립한다. 화면의 "교환 가능" 배지 기준이다. */
    public boolean exchangeable() {
        return !givableItemNames.isEmpty();
    }
}
