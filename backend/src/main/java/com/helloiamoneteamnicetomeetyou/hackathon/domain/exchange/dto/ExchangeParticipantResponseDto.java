package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.dto;

import java.util.List;
import java.util.UUID;

/**
 * 약속 화면의 한 줄이다.
 *
 * <p>{@code answered} 를 따로 두는 것은 "아직 안 고른 사람" 과 "고를 게 없다고 답한 사람" 을
 * 화면이 구분해야 하기 때문이다. 지금은 빈 선택을 저장할 수 없어서 둘이 같지만, 나중에 갈릴 때
 * 화면 코드를 고치지 않아도 되게 필드로 내려보낸다.
 */
public record ExchangeParticipantResponseDto(
        UUID userId,
        String username,
        List<Integer> slots,
        boolean answered,
        boolean arrived) {
}
