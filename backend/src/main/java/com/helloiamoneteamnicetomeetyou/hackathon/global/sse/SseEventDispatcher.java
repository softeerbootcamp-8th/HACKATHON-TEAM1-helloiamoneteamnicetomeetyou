package com.helloiamoneteamnicetomeetyou.hackathon.global.sse;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@link SseEventPublisher} 가 올린 이벤트를 받아 보낼 연결을 찾는다.
 *
 * <p><b>{@code AFTER_COMMIT} 인 이유.</b> 매칭을 저장하는 트랜잭션 안에서 바로 보내면, 그 뒤에
 * 예외가 나서 롤백돼도 화면은 이미 "매칭됐다"를 받은 뒤다. DB 에 없는 매칭이 화면에만 남는
 * 것인데, 되돌릴 방법이 없다. 커밋된 다음에 보내면 그 경우가 없어진다.
 *
 * <p><b>{@code fallbackExecution = true} 인 이유.</b> 이게 없으면 트랜잭션 밖에서 발행한 이벤트가
 * 조용히 사라진다. 스케줄러나 조회 전용 서비스에서 알림을 보내는 경우가 생길 텐데, 부르는 쪽이
 * 트랜잭션 안인지 아닌지를 신경 쓰게 만들고 싶지 않았다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventDispatcher {

    private final SseConnectionManager connectionManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSseEvent(SseEvent event) {
        Collection<SseConnection> targets = event.boothId() != null
                ? connectionManager.findByBooth(event.boothId())
                : connectionManager.findByUser(event.userId());

        if (targets.isEmpty()) {
            return;
        }

        log.debug("sse 발행: type={}, boothId={}, userId={}, 대상={}",
                event.type(), event.boothId(), event.userId(), targets.size());

        connectionManager.dispatch(targets, event.type(), event.data());
    }
}
