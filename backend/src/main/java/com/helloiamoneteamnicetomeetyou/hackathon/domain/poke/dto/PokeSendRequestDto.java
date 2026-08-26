package com.helloiamoneteamnicetomeetyou.hackathon.domain.poke.dto;

import java.util.UUID;

/**
 * 찔러보기를 보낸다.
 *
 * <p>내놓을 카드를 여기 담지 않는다. 보내는 사람의 보유 카드 <b>전부</b>가 묶음으로 가고
 * 받는 쪽이 그중 한 장을 고르는 흐름이다 (시안 desc 165:3677).
 *
 * @param userId          찔러보는 사람. 팀 규약대로 body 첫 필드다
 * @param targetUserId    찔러볼 상대
 * @param requestedItemId 상대에게서 받고 싶은 카드
 */
public record PokeSendRequestDto(UUID userId, UUID targetUserId, Long requestedItemId) {}
