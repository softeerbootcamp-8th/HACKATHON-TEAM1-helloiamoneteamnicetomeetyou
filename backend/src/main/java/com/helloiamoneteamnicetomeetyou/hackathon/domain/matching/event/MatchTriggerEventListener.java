package com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.event;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@code AFTER_COMMIT} 인 이유: {@code runMatching} 이 비동기라, 커밋 전에 다른 스레드에서
 * 먼저 돌면 방금 반영한 카드 변경을 못 보고 지나친다. 실제로 반영된 뒤에 돌게 미룬다.
 *
 * <p>같은 사용자에 대해 이 이벤트가 여러 번 겹쳐 발행돼도 문제 없다. {@code runMatching} 은
 * 매번 현재 상태를 새로 읽고, 같은 카드를 두 매칭이 동시에 예약하려 들면 낙관적 락이 막는다.
 */
@Component
@RequiredArgsConstructor
public class MatchTriggerEventListener {

    private final MatchingService matchingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchTrigger(MatchTriggerEvent event) {
        matchingService.runMatching(event.userId());
    }
}
