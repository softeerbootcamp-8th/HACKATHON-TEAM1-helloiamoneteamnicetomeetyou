package com.helloiamoneteamnicetomeetyou.hackathon.domain.exchange.event;

import com.helloiamoneteamnicetomeetyou.hackathon.domain.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 거절로 취소된 교환의 참가자들을 다시 매칭 풀에 들여보낸다.
 *
 * <p>{@code AFTER_COMMIT} 인 이유: 카드 예약을 푸는 것과 재매칭이 같은 트랜잭션이면, 재매칭이
 * (비동기라 다른 스레드에서 돈다) 커밋 전의 오래된 상태를 볼 수 있다. 실제로 풀린 뒤에 돌게
 * 커밋 이후로 미룬다.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRejectedEventListener {

    private final MatchingService matchingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExchangeRejected(ExchangeRejectedEvent event) {
        event.participantIds().forEach(matchingService::runMatching);
    }
}
