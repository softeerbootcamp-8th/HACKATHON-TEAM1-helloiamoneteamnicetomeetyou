package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.event;

import java.util.List;
import java.util.UUID;

/**
 * 참가자 거절로 교환이 취소됐다는 신호다. 재매칭을 시도할 사람 목록을 들고 있다.
 *
 * <p>거절한 사람도 포함한다. 재매칭 후보 쿼리가 같은 조합(상대 + 카드)을 다시 안 띄우도록
 * 이력을 걸러내기 때문에, 거절한 사람을 넣어도 방금 거절한 상대와 다시 붙지는 않는다.
 */
public record ExchangeRejectedEvent(List<UUID> participantIds) {}
