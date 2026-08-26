package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.entity.UserHaveItem;

/**
 * 내가 지금 등록해 둔 내놓을 카드 한 줄.
 *
 * <p>등록 화면이 제출 직전에 이 목록을 읽어 "서버에는 있는데 이번 선택에는 없는 카드" 를
 * 가려내고 해제 요청을 보낸다. 화면 상태는 새로고침에 사라지므로 서버가 유일한 기준이다.
 *
 * @param itemId   카드 식별자. 해제 요청의 경로 변수가 이 값이다
 * @param quantity 지금 등록해 둔 개수
 * @param reserved 교환에 예약되어 해제할 수 없는 카드인가
 */
public record HaveItemRegisteredResponseDto(Long itemId, Integer quantity, boolean reserved) {

    public static HaveItemRegisteredResponseDto from(UserHaveItem haveItem) {
        return new HaveItemRegisteredResponseDto(
                haveItem.getItem().getId(), haveItem.getQuantity(), haveItem.isReserved());
    }
}
