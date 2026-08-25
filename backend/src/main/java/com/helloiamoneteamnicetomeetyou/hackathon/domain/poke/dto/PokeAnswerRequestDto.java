package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.enums.PokeStatus;
import java.util.UUID;

/**
 * 받은 찔러보기에 답한다. 수락과 거절이 같은 엔드포인트다.
 *
 * <p>동사를 URL 에 넣지 않으려고 하나로 묶었다. 팀 규약이 "kebab-case 에 복수 명사" 라서
 * {@code /pokes/{id}/accept} 같은 경로를 만들지 않는다.
 *
 * @param userId       답하는 사람. 받은 사람 본인이어야 한다
 * @param status       {@code ACCEPTED} 또는 {@code REJECTED}
 * @param chosenItemId 수락할 때 고른 카드. 거절이면 없어도 된다
 */
public record PokeAnswerRequestDto(UUID userId, PokeStatus status, Long chosenItemId) {}
