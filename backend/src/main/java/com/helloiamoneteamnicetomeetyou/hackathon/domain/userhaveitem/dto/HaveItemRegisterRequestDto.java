package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto;

import java.util.UUID;

/**
 * 부스 관리자가 올린 상품 목록에서 내가 가진 카드를 등록할 때 쓴다.
 *
 * @param userId   클라이언트가 만들어 들고 다니는 값
 * @param itemId   등록할 카드
 * @param quantity 이번에 등록하는 개수. 1 이상이어야 한다
 */
public record HaveItemRegisterRequestDto(UUID userId, Long itemId, Integer quantity) {}
