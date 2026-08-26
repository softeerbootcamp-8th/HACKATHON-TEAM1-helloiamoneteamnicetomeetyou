package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event;

import java.util.UUID;

/**
 * 이 사용자를 대상으로 매칭을 다시 시도해 보라는 신호다.
 *
 * <p>보유·희망 카드 등록, 참가자 거절로 인한 재매칭 등 "이 사람의 매칭 가능성이 바뀌었다" 를
 * 뜻하는 자리는 전부 이 이벤트 하나로 모은다. 트랜잭션 커밋 후에 처리해야 하므로(카드 변경이
 * 실제로 반영된 뒤에 매칭이 그 상태를 봐야 한다) 직접 서비스를 부르지 않고 이벤트로 감싼다.
 */
public record MatchTriggerEvent(UUID userId) {}
