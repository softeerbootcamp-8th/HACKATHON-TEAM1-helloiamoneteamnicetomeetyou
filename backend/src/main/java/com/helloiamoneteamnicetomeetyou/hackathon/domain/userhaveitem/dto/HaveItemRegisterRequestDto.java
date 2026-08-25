package com.helloiamoneteamnicetomeetyou.hackathon.domain.userhaveitem.dto;

import java.util.UUID;

/**
 * 내가 내놓을 카드를 등록할 때 쓴다.
 *
 * @param userId   클라이언트가 만들어 들고 다니는 값
 * @param itemId   {@code GET /api/booths/{boothId}/items} 에서 받은 카드 식별자
 * @param quantity 지금 가진 개수. 1 이상이어야 한다
 */
public record HaveItemRegisterRequestDto(UUID userId, Long itemId, Integer quantity) {}
