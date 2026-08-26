package com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userwantitem.entity.UserWantItem;

/**
 * 내가 지금 등록해 둔 찾는 카드 한 줄.
 *
 * <p>쓰임은 {@code HaveItemRegisteredResponseDto} 와 같다. 찾는 카드에는 예약 개념이 없어서
 * {@code reserved} 자리가 없다.
 *
 * @param itemId   카드 식별자. 해제 요청의 경로 변수가 이 값이다
 * @param quantity 지금 등록해 둔 개수
 */
public record WantItemRegisteredResponseDto(Long itemId, Integer quantity) {

    public static WantItemRegisteredResponseDto from(UserWantItem wantItem) {
        return new WantItemRegisteredResponseDto(wantItem.getItem().getId(), wantItem.getQuantity());
    }
}
